package edu.virginia.speclab.juxta.author.view.search;

public class SearchOptions {
    private final String searchTerm;
    private final boolean wrapSearch;
    private final boolean searchAllFiles;
    
    public SearchOptions(final String word, boolean allFiles, boolean wrap) {
        this.searchTerm = word;
        this.searchAllFiles = allFiles;
        if ( allFiles ) {
            this.wrapSearch = false;
        } else {
            this.wrapSearch = wrap;
        }
    }

    public final String getSearchTerm() {
        return searchTerm;
    }

    public final boolean wrapSearch() {
        return wrapSearch;
    }

    public final boolean searchAllFiles() {
        return searchAllFiles;
    }
    
    
}
