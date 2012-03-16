/*
 *  Copyright 2002-2010 The Rector and Visitors of the
 *                      University of Virginia. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package edu.virginia.speclab.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

import javax.swing.BoundedRangeModel;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import edu.virginia.speclab.util.ImageLoader;

public class DualScrollingTextPanel extends JPanel {
    private WaterMarkedTextArea leftPanel, rightPanel;
    private LocationMarkStrip lineNumberStrip;
    private LocationMarkStrip lineNumberStripRight;
    private JViewport leftView, rightView;
    private JPanel centerBar;
    private JScrollBar rightScrollBar, leftScrollBar;
    private int leftPosition, rightPosition;
    private Renderer renderer;

    private Boolean showCenterStrip = true;

    // this value is used often, cached here for performance.
    private int lineHeight;

    public static int DEFAULT_LINE_HEIGHT = 15;
    private static int SCROLL_SPEED_BOOST = 5;

    private static final ImageLoader imageLoader = new ImageLoader(null);
    private static final BufferedImage NO_TEXT = imageLoader.loadImage("icons/no.text.gif");

    // given in lines
    private static int SCROLL_BLOCK_INCREMENT = 12;

    // in pixels
    private static int SCROLL_UNIT_INCREMENT = 15;

    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    public DualScrollingTextPanel() {
        initUI();
        addMouseWheelListener(new MouseWheeler());
        lineHeight = DEFAULT_LINE_HEIGHT;
    }

    private void initUI() {
        leftPanel = new WaterMarkedTextArea();
        rightPanel = new WaterMarkedTextArea();
        lineNumberStrip = new LocationMarkStrip(leftPanel, true, LocationMarkStrip.Position.LEFT);
        lineNumberStrip.setVisible(false);
        lineNumberStripRight = new LocationMarkStrip(rightPanel, true, LocationMarkStrip.Position.RIGHT);
        lineNumberStripRight.setVisible(false);

        leftView = new JViewport();
        leftView.setView(leftPanel);
        leftView.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        leftView.setOpaque(false);

        setBackground(Color.WHITE);

        rightView = new JViewport();
        rightView.setView(rightPanel);
        rightView.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        rightView.setOpaque(false);

        centerBar = new JPanel() {
            public void paint(Graphics g) {
                fireRender(g);
            }
        };
        //centerBar.setPreferredSize(new Dimension(50, 600));
        centerBar.setOpaque(false);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        leftScrollBar = new JScrollBar();
        leftScrollBar.setUnitIncrement(SCROLL_UNIT_INCREMENT);
        leftScrollBar.setVisible(false);
        add(leftScrollBar);

        add(lineNumberStrip);
        add(leftView);
        add(centerBar);
        add(rightView);
        add(lineNumberStripRight);

        rightScrollBar = new JScrollBar();
        rightScrollBar.setUnitIncrement(SCROLL_UNIT_INCREMENT);
        add(rightScrollBar);

        // update the scroll bar model as the window resizes
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                updateScrollBar();
            }
        });
    }
    
    public void resize() {
        int w = getWidth();
        int sbW = leftScrollBar.getWidth() + rightScrollBar.getWidth();
        int numW = lineNumberStrip.getWidth() + lineNumberStripRight.getWidth();
        w -= (sbW + numW); 
      
        int centerBarWidth = 150;
        leftView.setPreferredSize(new Dimension((w - centerBarWidth) / 2, getHeight()));
        rightView.setPreferredSize(new Dimension((w - centerBarWidth) / 2, getHeight()));
    }

    public boolean toggleCenterStripVisibility() {
        showCenterStrip = !showCenterStrip;
        centerBar.repaint();
        return showCenterStrip;
    }

    public void addScroller(AdjustmentListener scroller, int side) {
        if (scroller != null) {
            if (side == LEFT) {
                leftScrollBar.addAdjustmentListener(scroller);
            } else {
                rightScrollBar.addAdjustmentListener(scroller);
            }
        }
    }

    /**
     * Sets the font for this component and for the two scrolling text panels it contains. Note
     * that the font must be specified to get accurate results from <code>getLineHeight()</code>
     */
    @Override
    public void setFont(Font font) {
        // obtain the line height and store it in a final var
        if (leftPanel != null && rightPanel != null) {
            leftPanel.setFont(font);
            rightPanel.setFont(font);
            this.lineHeight = leftPanel.getFontMetrics(font).getHeight();
            if (this.lineHeight == 0)
                this.lineHeight = DEFAULT_LINE_HEIGHT; // fall back if unable to get line height

            if (lineNumberStrip != null) {
                lineNumberStrip.recalculateLineHeight();
            }
            if (lineNumberStripRight != null) {
                lineNumberStripRight.recalculateLineHeight();
            }

        }

        super.setFont(font);
    }

    public void scrollToPosition(float position, int side) {
        int scrollMax = getScrollMax();
        int targetPosition = Math.round((float) scrollMax * position);

        if (side == RIGHT)
            rightScrollBar.setValue(targetPosition);
        else
            leftScrollBar.setValue(targetPosition);
    }

    /**
     * Obtain the number of display lines of text in the specified panel.
     * @param which either <code>DocumentCompareView.LEFT</code> or <code>DocumentCompareView.RIGHT</code>
     * @return The total number of lines in the document, at the current font and window size.
     */
    public int getLineCount(int which) {
        JTextComponent textArea = (which == LEFT) ? leftPanel : rightPanel;

        int textHeight = textArea.getPreferredSize().height;
        return textHeight / lineHeight;
    }

    /**
     * obtain the y position given a character number
     * @param isBaseSearch 
     *
     */
    public int getCharacterPosition(int characterNumber, boolean isBaseSearch) {
        try {
            Rectangle comparandRect;
            Rectangle viewRect;
            if (isBaseSearch) {
                comparandRect = leftPanel.modelToView(characterNumber);
                viewRect = leftPanel.getVisibleRect();
            } else {
                comparandRect = rightPanel.modelToView(characterNumber);
                viewRect = rightPanel.getVisibleRect();
            }

            int viewLocation = comparandRect.y - viewRect.height / 2;
            //make sure the view location isn't out of bounds
            if (viewLocation < 0)
                viewLocation = 0;
            return viewLocation;
        } catch (BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 1;
        }
    }

    // figure out how big the scroll bar should be and adjust the thumb and block increment
    private void updateScrollBar() {
        // the new height of the window is not known until all AWT event messages
        // have been processed. Once it is known, update the scroll bar.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // update scroll bar           
                leftScrollBar.setVisibleAmount(getHeight());
                leftScrollBar.setBlockIncrement(lineHeight * SCROLL_BLOCK_INCREMENT);
                rightScrollBar.setVisibleAmount(getHeight());
                rightScrollBar.setBlockIncrement(lineHeight * SCROLL_BLOCK_INCREMENT);
            }
        });
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    private void fireRender(Graphics g) {
        if (renderer != null && showCenterStrip) {
            renderer.renderToSurface(g);
        }
    }

    public JTextComponent getLeftTextArea() {
        return leftPanel;
    }

    public JTextComponent getRightTextArea() {
        return rightPanel;
    }

    public int getCenterWidth() {
        return centerBar.getWidth();
    }

    /**
     * Obtain the height of a line of text displayed in this view.
     * @return The height of a line in pixels.
     */
    public int getLineHeight() {
        return lineHeight;
    }

    /**
     * Scrolls the left text area to the specified position.
     */
    public void scrollLeft(int scrollPosition) {
        leftPosition = (scrollPosition > 0) ? scrollPosition : 0;
        leftView.setViewPosition(new Point(0, leftPosition));
        lineNumberStrip.setScrollPosition(leftPosition);
    }

    /**
     * Scrolls the right text area to the specified position.
     */
    public void scrollRight(int scrollPosition) {
        rightPosition = (scrollPosition > 0) ? scrollPosition : 0;
        rightView.setViewPosition(new Point(0, rightPosition));
        lineNumberStripRight.setScrollPosition(rightPosition);
    }

    public void setLeftText(String text) {
        if (text == null) {
            leftPanel.setWatermark(true);
        } else {
            leftPanel.setWatermark(false);
        }
        
        resetScrolling();
        leftPanel.setText(text);
        leftPanel.setCaretPosition(0);
        updateScrollBar();
    }

    public void setScrollMax(int max) {
        //TODO 
        leftScrollBar.setMaximum(max);
        rightScrollBar.setMaximum(max);
    }

    public int getScrollMax() {
        return rightScrollBar.getMaximum();
    }

    /**
     * Scroll to this line of the left hand text, using the scroll
     * controller to do the scrolling.  
     * @param position 
     */
    public void scrollToLine(int position, int side) {
        if (side == RIGHT)
            rightScrollBar.setValue(position * lineHeight);
        else
            leftScrollBar.setValue(position * lineHeight);
    }

    public void resetScrolling() {
        scrollLeft(0);
        scrollRight(0);
        rightScrollBar.setValue(0);
    }

    public void setRightText(String text) {
        if (text == null) {
            rightPanel.setWatermark(true);
        } else {
            rightPanel.setWatermark(false);
        }

        resetScrolling();
        rightPanel.setText(text);
        rightPanel.setCaretPosition(0);
        updateScrollBar();
    }

    public int getLeftPosition() {
        return leftView.getViewPosition().y;
    }

    public int getRightPosition() {
        return rightView.getViewPosition().y;
    }

    private class MouseWheeler implements MouseWheelListener {
        public void mouseWheelMoved(MouseWheelEvent e) {
            rightScrollBar.setValue(rightScrollBar.getValue() + (e.getUnitsToScroll() * SCROLL_SPEED_BOOST));
        }
    }

    public class DefaultScroller implements AdjustmentListener {
        public void adjustmentValueChanged(AdjustmentEvent e) {
            int position = e.getValue();
            scrollLeft(position);
            scrollRight(position);
        }
    }

    public boolean isLineNumberStripVisible() {
        return lineNumberStrip.isVisible();
    }

    public void showLineNumberStrip(boolean visible) {
        lineNumberStrip.setVisible(visible);
        lineNumberStripRight.setVisible(visible);
    }

    public void setLocationMarkStripColor(Color backgroundColor) {
        this.lineNumberStrip.setBackground(backgroundColor);
        this.lineNumberStripRight.setBackground(backgroundColor);
    }

    public LocationMarkStrip getLineNumberStrip() {
        return lineNumberStrip;
    }

    public LocationMarkStrip getLineNumberStripRight() {
        return lineNumberStripRight;
    }

    public void addScrollBarListener(ChangeListener listener) {
        rightScrollBar.getModel().addChangeListener(listener);
    }

    public float getScrollPosition() {
        BoundedRangeModel model = rightScrollBar.getModel();
        float range = model.getMaximum() - model.getMinimum();
        float value = model.getValue() - model.getMinimum();
        if (range > 0f) {
            return value / range;
        }
        return 0f;
    }

    public void updateScrollBarPosition(int targetPosition, int side) {
        if (side == RIGHT) {
            rightScrollBar.setValue(targetPosition);
        } else {
            leftScrollBar.setValue(targetPosition);
        }
    }

    public int getTextSize(boolean left) {
        // returns the y-dimension of the left hand side
        Dimension d;
        if (left) {
            d = leftPanel.getPreferredSize();
        } else {
            d = rightPanel.getPreferredSize();
        }
        return d.height;
    }

    public void leftScrollBarVisible(boolean b) {
        leftScrollBar.setVisible(b);
    }
    
    /**
     * Text area with a set of instructions as a watermark
     */
    private class WaterMarkedTextArea extends JTextArea {
        private boolean display;

        public WaterMarkedTextArea() {
            setOpaque(false);
            setEditable(false);
            setFocusable(true);
            setWrapStyleWord(true);
            setLineWrap(true);
            setMargin( new Insets(0, 5, 0, 5));
        }

        public void setWatermark(boolean display) {
            this.display = display;
        }

        public void paint(Graphics g) {
            if (display)
                paintWaterMark(g);
            super.paint(g);
        }

        private void paintWaterMark(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.drawImage(NO_TEXT, 25, 80, NO_TEXT.getWidth(), NO_TEXT.getHeight(), null);
        }
    }

}
