/*
 *  Copyright 2002-2011 The Rector and Visitors of the
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
package edu.virginia.speclab.juxta.author.view.collation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JTextPane;
import javax.swing.ToolTipManager;

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.OffsetRange.Space;
import edu.virginia.speclab.diff.document.PageBreakData;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;

public class PageBreakManager  implements ComponentListener {
    private JuxtaDocument document;
    private JTextPane textPane;
    private List<PageBreak> breakPoints; 
    
    public PageBreakManager(JTextPane pane ) {
        this.textPane = pane;
        this.breakPoints = Collections.synchronizedList( new ArrayList<PageBreak>() );
    }
    
    public void initialize( JuxtaDocument document ) {
        this.document = document;
        if ( document == null ) {
            return;
        }
        
        createPageBreaks(); 
    }
    
    private void createPageBreaks() {
        int maxX = this.textPane.getSize().width - RenderingConstants.MARGIN_SIZE;
        int minX = RenderingConstants.PB_MARGIN_SIZE;
        for ( PageBreakData pbd : this.document.getPageBreaks() ) {
            try {
                OffsetRange range = pbd.getRange();
                int breakPoint = range.getStartOffset(Space.ACTIVE);
                Rectangle rect = this.textPane.modelToView(breakPoint);
                this.breakPoints.add(  new PageBreak(minX, maxX, rect, pbd.getLabel()));
                
            } catch (Exception e) {
                
            }
        }
    }
    
    public void paint(Graphics2D g2) {        
        for (PageBreak pb : this.breakPoints ) {
            pb.paint(g2);
        }
    }
    
    public void rollover(Point point) {
        boolean changed = false;
        PageBreak hitPb = null;
        for ( PageBreak pb : this.breakPoints ) {
            boolean old = pb.isHighlighted();
            boolean hit = pb.isHit(point);
            pb.setHighlighted( hit );
            if (old != pb.isHighlighted()) {
                changed = true;
            }
            if ( hit ) {
                hitPb = pb;
            }
        }
        
        if ( changed ) {
            this.textPane.repaint();
            if ( hitPb != null ) {
                this.textPane.setToolTipText(hitPb.getLabel());
                ToolTipManager.sharedInstance().setInitialDelay(0);
                ToolTipManager.sharedInstance().setReshowDelay(0);
                ToolTipManager.sharedInstance().setDismissDelay(5*60*1000);
            } else {
                this.textPane.setToolTipText( null );
            }
        }
    }
    
    public void componentResized(ComponentEvent arg0) {
        this.breakPoints.clear();
        createPageBreaks();
        this.textPane.repaint();
    }

    // No-op for all of these....
    public void componentHidden(ComponentEvent arg0) {}
    public void componentMoved(ComponentEvent arg0) {}
    public void componentShown(ComponentEvent arg0) {}
    
    /**
     * class used to draw page break
     * @author loufoster
     *
     */
    private static class PageBreak {
        private String label;
        private Line2D pageLine;
        private RoundRectangle2D end;
        private boolean highlight = false;
        private Rectangle hitRect;
        private Point breakPoint;
        private int lineHeight;

        private static final float[] DASH_DATA = {4.0f};
        private static final Stroke[] BREAK_STROKE = { 
            new BasicStroke( RenderingConstants.STROKE_WIDTH, BasicStroke.CAP_BUTT,  
                BasicStroke.JOIN_MITER, 10.0f, DASH_DATA, 0.0f),
            new BasicStroke(),
        }; 
        private static final Stroke FAT_STROKE = new BasicStroke(3);
        private static final Color[] BREAK_COLOR = { new Color(200,200,200),  new Color(50,50,255)};
        private static final Color END_STROKE_COLOR = new Color(135,146,255);
        private static final Color END_FILL_COLOR = new Color(213,217,255);
        
        
        public PageBreak( int minX, int maxX, Rectangle breakRect, String label ) {
            this.label = label;
            int breakY = breakRect.y+breakRect.height+1;
            this.breakPoint = new Point(breakRect.x, breakY);
            this.pageLine = new Line2D.Float( minX, breakY, maxX, breakY);
            this.end = new RoundRectangle2D.Float( 0, breakY-4, 8,8, 6,6);
            this.hitRect = new Rectangle( breakRect.x-5, breakRect.y, 10, breakRect.height);
            this.lineHeight = breakRect.height;
        }
        public void paint(Graphics2D g2) {
            
            g2.setComposite(RenderingConstants.DRAW_MODE);
            int idx = 0;
            if ( this.highlight ) {
                idx = 1;
            }
            
            // render the end-of-line dot marker
            g2.setPaint( END_FILL_COLOR );
            g2.fill(this.end);
            g2.setPaint( END_STROKE_COLOR );
            g2.setStroke( BREAK_STROKE[1] );
            g2.draw(this.end);
            
            // draw horizontal line where break occurs
            g2.setStroke( BREAK_STROKE[idx] );
            g2.setPaint( BREAK_COLOR[idx] );
            g2.draw( this.pageLine );
            
            // draw a small arrow showing where on the line
            // the PB occurred
            if ( this.highlight ) {
                g2.draw( new Line2D.Float( this.breakPoint.x, this.breakPoint.y, 
                                           this.breakPoint.x, this.breakPoint.y-this.lineHeight ) ); 
            } 
            
            g2.setStroke( FAT_STROKE );
            g2.draw( new Line2D.Float( this.breakPoint.x, this.breakPoint.y-1, 
                                       this.breakPoint.x, this.breakPoint.y ) ); 
        }
        
        public final String getLabel() {
            return this.label;
        }
        
        public boolean isHit(Point pt) {
            return this.end.contains(pt) || this.hitRect.contains(pt);
        }
        
        public boolean isHighlighted() {
            return this.highlight;
        }
        
        public void setHighlighted( boolean lit) {
            this.highlight = lit;
        }
    }
}
