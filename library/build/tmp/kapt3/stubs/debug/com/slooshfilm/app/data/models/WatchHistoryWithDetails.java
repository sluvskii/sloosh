package com.slooshfilm.app.data.models;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u0019\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\u0004\b\u0006\u0010\u0007J\t\u0010\f\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010\r\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u001f\u0010\u000e\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005H\u00c6\u0001J\u0013\u0010\u000f\u001a\u00020\u00102\b\u0010\u0011\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0012\u001a\u00020\u0013H\u00d6\u0001J\t\u0010\u0014\u001a\u00020\u0015H\u00d6\u0001R\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0018\u0010\u0004\u001a\u0004\u0018\u00010\u00058\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u0016"}, d2 = {"Lcom/slooshfilm/app/data/models/WatchHistoryWithDetails;", "", "watchHistory", "Lcom/slooshfilm/app/data/models/WatchHistoryItem;", "movie", "Lcom/slooshfilm/app/data/model/Movie;", "<init>", "(Lcom/slooshfilm/app/data/models/WatchHistoryItem;Lcom/slooshfilm/app/data/model/Movie;)V", "getWatchHistory", "()Lcom/slooshfilm/app/data/models/WatchHistoryItem;", "getMovie", "()Lcom/slooshfilm/app/data/model/Movie;", "component1", "component2", "copy", "equals", "", "other", "hashCode", "", "toString", "", "library_debug"})
public final class WatchHistoryWithDetails {
    @androidx.room.Embedded()
    @org.jetbrains.annotations.NotNull()
    private final com.slooshfilm.app.data.models.WatchHistoryItem watchHistory = null;
    @androidx.room.Relation(parentColumn = "movieId", entityColumn = "url")
    @org.jetbrains.annotations.Nullable()
    private final com.slooshfilm.app.data.model.Movie movie = null;
    
    public WatchHistoryWithDetails(@org.jetbrains.annotations.NotNull()
    com.slooshfilm.app.data.models.WatchHistoryItem watchHistory, @org.jetbrains.annotations.Nullable()
    com.slooshfilm.app.data.model.Movie movie) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.slooshfilm.app.data.models.WatchHistoryItem getWatchHistory() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.slooshfilm.app.data.model.Movie getMovie() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.slooshfilm.app.data.models.WatchHistoryItem component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.slooshfilm.app.data.model.Movie component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.slooshfilm.app.data.models.WatchHistoryWithDetails copy(@org.jetbrains.annotations.NotNull()
    com.slooshfilm.app.data.models.WatchHistoryItem watchHistory, @org.jetbrains.annotations.Nullable()
    com.slooshfilm.app.data.model.Movie movie) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}