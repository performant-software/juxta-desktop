package edu.virginia.speclab.juxta.author.view.search;

public interface SearchListener {
    void handleSearch(final SearchOptions opts);
    void clearSearch();
}
