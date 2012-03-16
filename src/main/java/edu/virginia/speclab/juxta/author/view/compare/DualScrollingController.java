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

package edu.virginia.speclab.juxta.author.view.compare;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.OffsetRange.Space;
import edu.virginia.speclab.ui.DualScrollingTextPanel;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * This class controls the dual scrolling action for a <code>DocumentCompareView</code> object.
 * 
 * @author Nick
 *
 */
class DualScrollingController implements AdjustmentListener {
    // the view which is being controlled
    private DocumentCompareView documentCompareView;

    // array of ScollCommand objects 
    private List<ScrollCommand> scrollData;

    private List<Integer> leftOffsetArray;
    private List<Integer> rightOffsetArray;

    // maps scroll positions to base text offsets
    private Map<Integer, Integer> offsetMap;

    // used when controlling scrolling to calculate scroll delta
    private int lastPosition;

    public DualScrollingController(DocumentCompareView documentCompareView) {
        this.documentCompareView = documentCompareView;
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        if (this.documentCompareView.getScrollMode() == DocumentCompareView.SCROLL_MODE_LINKED) {
            scrollTo(e.getValue());
        }
    }

    /**
     * Call this function whenever the dimensions of the window, the text font size, or the 
     * contents of the view panels change. All of these things require a recalculation of 
     * scrolling data.
     */
    public void updateScrollData() {
        SimpleLogger.logInfo("loading scroll data");

        DualScrollingTextPanel dualTextPanel = documentCompareView.getDualTextPanel();

        // lookup the length in lines of these texts
        int leftLineCount = dualTextPanel.getLineCount(DifferenceConnector.LEFT);
        int rightLineCount = dualTextPanel.getLineCount(DifferenceConnector.RIGHT);
        int lineHeight = dualTextPanel.getLineHeight();

        // allocate the command buffer
        int initialSize = (leftLineCount > rightLineCount) ? leftLineCount : rightLineCount;
        this.leftOffsetArray = new ArrayList<Integer>();
        this.rightOffsetArray = new ArrayList<Integer>();
        this.scrollData = new ArrayList<ScrollCommand>();
        this.offsetMap = new HashMap<Integer, Integer>(initialSize);

        // get ready to loop
        int leftPosition = 0, rightPosition = 0, offset = 0;

        // loop while one document or the other still has lines
        while (leftPosition < leftLineCount || rightPosition < rightLineCount) {
            //set up the scroll array -- a list mapping the lines in the left
            //hand side to the scroll amounts needed to show that line
            this.leftOffsetArray.add(leftPosition, new Integer(lineHeight * this.scrollData.size()));
            this.rightOffsetArray.add(rightPosition, new Integer(lineHeight * this.scrollData.size()));
            
            // default command
            ScrollCommand command = ScrollCommand.GO_COMMAND;

            // if we are still in the midst of both docs
            if (leftPosition < leftLineCount && rightPosition < rightLineCount) {
                // get the connector at this position
                DifferenceConnector connector = documentCompareView
                    .getConnector(leftPosition, DifferenceConnector.LEFT);

                if (connector == null) {
                    connector = documentCompareView.getConnector(rightPosition, DifferenceConnector.RIGHT);
                }

                // figure out who should scroll
                command = generateScrollCommand(connector, leftPosition, rightPosition);

                if (connector != null) {
                    // record where we are
                    offset = connector.getBaseTextOffset();
                } else
                    offset = 0;

                // take into account the effect on the offsets of the scroll command
                if (command.equals(ScrollCommand.LEFT_STOP_COMMAND)) {
                    rightPosition++;
                } else if (command.equals(ScrollCommand.RIGHT_STOP_COMMAND)) {
                    leftPosition++;
                } else {
                    leftPosition++;
                    rightPosition++;
                }
            }
            // if the left document is at the end
            else if (leftPosition >= leftLineCount) {
                command = ScrollCommand.LEFT_STOP_COMMAND;
                rightPosition++;
            }
            // if the right document is at the end
            else if (rightPosition >= rightLineCount) {
                command = ScrollCommand.RIGHT_STOP_COMMAND;
                leftPosition++;
            }

            // now map this position on the scroll bar to an offset in the base text
            // so that we can move the scrollbar based on text offsets later on
            this.offsetMap.put(new Integer(offset), new Integer(this.scrollData.size()));

            // add the command to the buffer, this will later be used to control 
            // the scrolling of the two documents
            this.scrollData.add(command);
        }
        
        // hack? after the above, 0 is usually set to some large number
        // Without this, search that doesnt find any offsets in this map
        // until it hits 0, will scroll the doc to some point near the end.
        this.offsetMap.put(0,0);

        //clear the extra elements
        while (this.leftOffsetArray.size() != leftPosition) {
            this.leftOffsetArray.remove(leftPosition);
        }
        while (this.rightOffsetArray.size() != rightPosition) {
            this.rightOffsetArray.remove(rightPosition);
        }

        // the max size of the scroll model equals the number of commands * line height in 
        // pixels
        int scrollMax = dualTextPanel.getLineHeight() * this.scrollData.size();
        dualTextPanel.setScrollMax(scrollMax);
    }

    // the goal is of this method is to keep the two documents lined up on the screen. We 
    // achieve this by scrolling them at different rates.
    private void scrollTo(int position) {
        DualScrollingTextPanel dualTextPanel = documentCompareView.getDualTextPanel();
        // by default, scroll both panels the entire amount of the position delta from
        // the scroll bar
        int positionDelta = position - lastPosition;

        // if the scroll bar has not actually moved
        if (positionDelta == 0)
            return;

        // there is no scroll data available, just scroll both at same rate
        if (scrollData == null || scrollData.isEmpty()) {
            SimpleLogger.logInfo("No scroll data found, using fallback scroll method.");
            dualTextPanel.scrollLeft(dualTextPanel.getLeftPosition() + positionDelta);
            dualTextPanel.scrollRight(dualTextPanel.getRightPosition() + positionDelta);

            // record the position         
            lastPosition = position;
            return;
        }

        // initialize scroll delta accumulators
        int leftScrollAmount = 0;
        int rightScrollAmount = 0;

        // positive if scroll down, negative scrolling up
        int direction = (positionDelta > 0) ? 1 : -1;

        // loop through all the lines passed over by this movement and stop the scrolling
        // (subtract from movement) whenever we encounter a stop command in the scroll
        // data.                        
        for (int offset = 0; offset < Math.abs(positionDelta); offset++) {
            // the line for this iteration         
            int line = (lastPosition + (offset * direction)) / dualTextPanel.getLineHeight();

            // if the line is in range for this pane, get the scroll command
            ScrollCommand command;
            if (line >= scrollData.size() || line < 0)
                command = ScrollCommand.GO_COMMAND;
            else
                command = (ScrollCommand) scrollData.get(line);

            // scroll left panel if not told to stop
            if (command.equals(ScrollCommand.LEFT_STOP_COMMAND) == false) {
                if (direction > 0)
                    leftScrollAmount++;
                else
                    leftScrollAmount--;
            }

            // scroll right panel if not told to stop
            if (command.equals(ScrollCommand.RIGHT_STOP_COMMAND) == false) {
                if (direction > 0)
                    rightScrollAmount++;
                else
                    rightScrollAmount--;
            }
        }

        //SimpleLogger.logInfo("delta="+positionDelta+" left="+leftScrollAmount+" right="+rightScrollAmount );

        // after we have calculated the scroll amounts, apply them to the panels
        dualTextPanel.scrollLeft(dualTextPanel.getLeftPosition() + leftScrollAmount);
        dualTextPanel.scrollRight(dualTextPanel.getRightPosition() + rightScrollAmount);

        // record the scroll bar position         
        lastPosition = position;

        // redraw the connectors
        documentCompareView.repaint();
    }

    public void scrollToWithOffset(int offset, boolean isBaseSelected) {
        Integer mappedValue;
        if (isBaseSelected)
            mappedValue = (Integer) leftOffsetArray.get(offset);
        else
            mappedValue = (Integer) rightOffsetArray.get(offset);
        //scrollTo(mappedValue.intValue());
        documentCompareView.setScrollPosition(mappedValue.intValue(), DocumentCompareView.RIGHT);
    }

    // this method controls the scrolling, attempting to minimize the distance between
    // the left and right brackets of the connector 
    private ScrollCommand generateScrollCommand(DifferenceConnector connector, int leftPosition, int rightPosition) {
        if (connector != null) {
            int lineHeight = documentCompareView.getDualTextPanel().getLineHeight();

            int leftScreenPosition = leftPosition * lineHeight;
            int rightScreenPosition = rightPosition * lineHeight;

            // Figure out bracket extents
            int leftTop = connector.getLeftPosition() - leftScreenPosition;
            int leftBottom = leftTop + connector.getLeftLength();
            int rightTop = connector.getRightPosition() - rightScreenPosition;
            int rightBottom = rightTop + connector.getRightLength();

            // Try to keep the bottoms of the brackets aligned, stopping when the other
            // side needs to catch up.
            if (leftBottom < rightBottom) {
                return ScrollCommand.LEFT_STOP_COMMAND;
            } else if (leftBottom > rightBottom) {
                return ScrollCommand.RIGHT_STOP_COMMAND;
            }
        }

        return ScrollCommand.GO_COMMAND;
    }

    // This takes the character offset and turns it into a scroll position. The hash table of 
    // positions only contain the beginning of each location, but this may be called with any
    // character location.
    private Integer findHighestPositionLessThanOffset(int offset) {
        //System.out.println("Find pos for offset: " + offset);
        while (offset >= 0) {
            Integer position = (Integer) offsetMap.get(new Integer(offset));
            if (position == null) {
                --offset;
            } else {
                //System.out.println("Found match at: " + position);
                return position;
            }
        }
        return null;
    }

    public int getScrollPosition(Difference difference) {
        OffsetRange baseRange = difference.getBaseRange();
        Integer position = null;
        if (baseRange.getLength(Space.ORIGINAL) > 0) {
            int baseOffset = difference.getOffset(Difference.BASE);
            position = findHighestPositionLessThanOffset(baseOffset);
        }

        if (position == null) {
            int witnessOffset = difference.getOffset(Difference.WITNESS);
            position = findHighestPositionLessThanOffset(witnessOffset);

            if (position == null)
                return 0;
        }

        return position.intValue();
    }

    //returns the size of the offset array
    public int getOffsetSize(boolean isBaseSearch) {
        if (isBaseSearch)
            return leftOffsetArray.size();
        else
            return rightOffsetArray.size();
    }

}
