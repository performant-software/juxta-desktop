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

package edu.virginia.speclab.juxta.author.view.collation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.ui.WrappingTextLabel;

public final class NoteBox {
    public enum Type {TARGETED, MARGIN};
    
    private Type type;
    private boolean highlight;
    private final OffsetRange srcRange;
    private final String noteText;
    private int rightMargin;
    private Point origin;
    private Point position;
    private List<Line2D> callout;
    private Rectangle2D dot;
    private RoundRectangle2D box;
    private RoundRectangle2D shadow;
    private WrappingTextLabel note;
    private int noteTextStartOffset;
    
    private static final float[] DASH_DATA = {2.0f};
    private static final Color NOTE_LIT_COLOR = new Color(254,254,56);
    private static final Color NOTE_COLOR = new Color(255,252,200);
    private static final Color SHADOW_COLOR = new Color(120,120,120);
    private static final Color NOTE_BORDER_COLOR = new Color(188,169,2);
    private static final Stroke BORDER_STROKE = new BasicStroke();
    private static final Stroke DASH_STROKE = new BasicStroke(
        RenderingConstants.STROKE_WIDTH, BasicStroke.CAP_BUTT,  
        BasicStroke.JOIN_MITER, 10.0f, DASH_DATA, 0.0f);  

    public NoteBox( final String txt, OffsetRange srcRange, Point srcPoint, Point tgtPoint) {
        this.srcRange = srcRange;
        this.noteText = normalizeText(txt);
        this.origin = srcPoint;
        this.position = tgtPoint;
        this.type = Type.TARGETED;
    }
    
    public void setType(Type t) {
        this.type = t;
    }
    
    public Type getType() {
        return this.type;
    }
    
    private String normalizeText(String txt) {
        String out = txt.replaceAll("\n", " ");
        out = out.replaceAll("\t", " ");
        out = out.replaceAll(" +", " ");
        return out.trim();
    }
    
    public void initialize(FontRenderContext fontRenderContext, final int rightMargin) {
        this.rightMargin = rightMargin;
        int txtW = RenderingConstants.BOX_WIDTH-4;
        this.note = new WrappingTextLabel(  this.noteText, JuxtaUserInterfaceStyle.SMALL_FONT, 
            Color.BLACK, txtW, 15, fontRenderContext  );   
        this.note.setLocation(this.position.x+4, this.position.y+2);
        createComponents();
    }

    public void move(int dX, int dY) {
        this.position.x += dX;
        this.position.y += dY;
        createComponents();
    }
    
    private void createComponents() {
              
        // create an array of lines that make up the callout.
        // if the start and end points are at the same Y location,
        // just draw a straight line. If not, draw a straight line to the
        // margin, followed by a diagonal to the end
        this.callout = new ArrayList<Line2D>();
        if ( this.origin.y == this.position.y ) {
            this.callout.add( new Line2D.Float( this.origin.x, this.origin.y, 
                this.position.x, this.position.y ) ); 
        } else {
            this.callout.add( new Line2D.Float( this.origin.x, this.origin.y, 
                this.rightMargin, this.origin.y ) ); 
            this.callout.add( new Line2D.Float( this.rightMargin, this.origin.y, 
                this.position.x, this.position.y ) ); 
        }
        
        // for targeted notes, create a dot at the start of the callout
        if ( this.type.equals(Type.TARGETED)) {
            this.dot = new Rectangle2D.Float( this.origin.x-2, this.origin.y - 3,4,6);
        } else {
            // for non targeted links, create an arrowy looking thing
            for (int sz=3; sz>=0; sz-- ) {
                this.callout.add( new Line2D.Float( this.origin.x+sz, this.origin.y-sz , 
                    this.origin.x+sz, this.origin.y+sz ) );
            }
        }
        
        // a box around the text
        int height = (int) (this.note.getHeight()+4);
        int y = Math.max(4, this.position.y-height/2);
        this.box = new RoundRectangle2D.Float( this.position.x, y,
            RenderingConstants.BOX_WIDTH, height, 6, 6);
        this.shadow = new RoundRectangle2D.Float( this.position.x+2, y+2,
            RenderingConstants.BOX_WIDTH, height, 6, 6);
        
        // stick the note inside of the box
        this.note.setLocation(this.position.x+4, y+2);
    }
    
    public OffsetRange getSourceRange() {
        return this.srcRange;
    }
    
    public void paint(Graphics2D g2 ) {
        g2.setComposite(RenderingConstants.DRAW_MODE);
        g2.setStroke( DASH_STROKE );
        if ( this.highlight ) {
            g2.setStroke( BORDER_STROKE );
        }
        g2.setPaint( NOTE_BORDER_COLOR );
        for ( Line2D line : this.callout ) {
            g2.draw(line); 
        }
        
        if ( this.type.equals(Type.TARGETED)) {
            g2.fill(this.dot);  
        }
                
        g2.setPaint( SHADOW_COLOR );
        g2.fill(this.shadow );
        
        
        g2.setPaint( NOTE_COLOR);
        if ( this.highlight ) {
            g2.setPaint( NOTE_LIT_COLOR );
        }
        g2.fill(this.box );
        g2.setStroke( BORDER_STROKE );
        g2.setPaint( NOTE_BORDER_COLOR );
        g2.draw(this.box);
        
     
        this.note.paint(g2);
    }

    public void setHighlight(boolean hightlight) {
        this.highlight = hightlight;
    }

    public boolean isHighlight() {
        return highlight;
    }

    public Rectangle getBoundingBox() {
        return this.box.getBounds();
    }

    public boolean isHit(Point point) {
        return this.box.getBounds().contains(point);
    }

    public void setNoteTextStartOffset(int start) {
        this.noteTextStartOffset = start;
    }
    public int getNoteTextStartOffset() {
        return this.noteTextStartOffset;
    }
}
