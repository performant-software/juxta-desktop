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
 
package edu.virginia.speclab.diff.document;

import edu.virginia.speclab.exceptions.ReportedException;

/**
 *
 * @author ben
 */
public class OffsetMap {
    private int _size;
    private int[] _sourceToTargetMap;
    private int[] _targetToSourceMap;
    
    public OffsetMap(int size) {
        _sourceToTargetMap = new int[size + 1]; // the "+ 1" is for covering mapping the offset at length()
        _targetToSourceMap = new int[size + 1];
        _size = size;
    }

    public OffsetMap(int[] sourceToTargetMap, int[] targetToSourceMap) {
        _sourceToTargetMap = sourceToTargetMap;
        _targetToSourceMap = targetToSourceMap;
        _size = _sourceToTargetMap.length;
    }

    public int getSize() {
        return _size;
    }

    public void mapSourceToTarget(int sourceOffset, int targetOffset) {
        _sourceToTargetMap[sourceOffset] = targetOffset;
    }

    public void mapTargetToSource(int targetOffset, int sourceOffset) {
        _targetToSourceMap[targetOffset] = sourceOffset;
    }

    public int getSourceOffset(int targetOffset) throws ReportedException {
        try 
        {
            return _targetToSourceMap[targetOffset];        
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new ReportedException(e, "offset " + targetOffset + " is larger than size " + _size);
        }
    }

    public int getTargetOffset(int sourceOffset) throws ReportedException {
        try
        {
            return _sourceToTargetMap[sourceOffset];
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new ReportedException(e, "offset " + sourceOffset + " is larger than size " + _size);
        }
    }
}
