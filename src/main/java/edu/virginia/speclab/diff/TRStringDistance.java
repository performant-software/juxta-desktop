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

package edu.virginia.speclab.diff;

import edu.virginia.speclab.diff.document.DocumentModel;


/**
 * Copyright 2002-2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Edit distance  class
 */
public final class TRStringDistance 
{
    private static final int MAX_DISTANCE_COMPARISON = 2500;
	private char[] sa;
    private int n;
    private int[][][] cache=new int[30][][];

    private int distance;

    /**
     * Optimized to run a bit faster than the static getDistance().
     * In one benchmark times were 5.3sec using ctr vs 8.5sec w/ static method, thus 37% faster.
     */
    public TRStringDistance( DocumentModel baseDocument, DocumentModel witnessDocument, Difference difference)
    {
        String baseString = baseDocument.getSubString(difference.getOffset(Difference.BASE),difference.getLength(Difference.BASE));
        String witnessString = witnessDocument.getSubString(difference.getOffset(Difference.WITNESS),difference.getLength(Difference.WITNESS));

        if(!(baseString.length() * witnessString.length() > MAX_DISTANCE_COMPARISON))
        {
	        sa=baseString.toCharArray();
	        n=sa.length; 
	        
	        distance = computeDistance(witnessString);
        }
        else
        {
        	distance = MAX_DISTANCE_COMPARISON;
        }
    }
    
    public int getDistance()
    {
        return distance;
    }


    //*****************************
     // Compute Levenshtein distance
     //*****************************
      private int computeDistance (String other) {
          int d[][]; // matrix
          int cost; // cost

          // Step 1
          final char[] ta=other.toCharArray();
          final int m=ta.length;
          if (n==0) {
              return m;
          }
          if (m==0) {
              return n;
          }

          if (m>=cache.length) {
              d=form(n, m);
          }
          else if (cache[m]!=null) {
              d=cache[m];
          }
          else {
              d=cache[m]=form(n, m);

              // Step 3

          }
          for (int i=1; i<=n; i++) {
              final char s_i=sa[i-1];

              // Step 4

              for (int j=1; j<=m; j++) {
                  final char t_j=ta[j-1];

                  // Step 5

                  if (s_i==t_j) { // same
                      cost=0;
                  }
                  else { // not a match
                      cost=1;

                      // Step 6

                  }
                  d[i][j]=min3(d[i-1][j]+1, d[i][j-1]+1, d[i-1][j-1]+cost);

              }

          }

          // Step 7
          return d[n][m];

      }


    /**
     *
     */
    private static int[][] form (int n, int m) {
        int[][] d=new int[n+1][m+1];
        // Step 2

        for (int i=0; i<=n; i++) {
            d[i][0]=i;

        }
        for (int j=0; j<=m; j++) {
            d[0][j]=j;
        }
        return d;
    }


    //****************************
     // Get minimum of three values
     //****************************
      private static int min3 (int a, int b, int c) {
          int mi=a;
          if (b<mi) {
              mi=b;
          }
          if (c<mi) {
              mi=c;
          }
          return mi;

      }
    
}
