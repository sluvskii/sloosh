package com.slooshfilm.app.data.repository;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0017\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0004\b\u0006\u0010\u0007J\u0018\u0010\b\u001a\u0004\u0018\u00010\t2\u0006\u0010\n\u001a\u00020\u000bH\u0086@\u00a2\u0006\u0002\u0010\fJ&\u0010\r\u001a\u00020\u000e2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0010H\u0086@\u00a2\u0006\u0002\u0010\u0012J\u0012\u0010\u0013\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00160\u00150\u0014J\u0016\u0010\u0017\u001a\u00020\u000e2\u0006\u0010\u0018\u001a\u00020\u0019H\u0086@\u00a2\u0006\u0002\u0010\u001aR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001b"}, d2 = {"Lcom/slooshfilm/app/data/repository/WatchHistoryRepository;", "", "watchHistoryDao", "Lcom/slooshfilm/app/data/WatchHistoryDao;", "movieDao", "Lcom/slooshfilm/app/data/MovieDao;", "<init>", "(Lcom/slooshfilm/app/data/WatchHistoryDao;Lcom/slooshfilm/app/data/MovieDao;)V", "getWatchHistory", "Lcom/slooshfilm/app/data/models/WatchHistoryItem;", "movieId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "saveWatchHistory", "", "position", "", "duration", "(Ljava/lang/String;JJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getWatchHistoryWithDetails", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/slooshfilm/app/data/models/WatchHistoryWithDetails;", "insertMovie", "movie", "Lcom/slooshfilm/app/data/model/Movie;", "(Lcom/slooshfilm/app/data/model/Movie;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "library_debug"})
public final class WatchHistoryRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.slooshfilm.app.data.WatchHistoryDao watchHistoryDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.slooshfilm.app.data.MovieDao movieDao = null;
    
    public WatchHistoryRepository(@org.jetbrains.annotations.NotNull()
    com.slooshfilm.app.data.WatchHistoryDao watchHistoryDao, @org.jetbrains.annotations.NotNull()
    com.slooshfilm.app.data.MovieDao movieDao) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getWatchHistory(@org.jetbrains.annotations.NotNull()
    java.lang.String movieId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.slooshfilm.app.data.models.WatchHistoryItem> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveWatchHistory(@org.jetbrains.annotations.NotNull()
    java.lang.String movieId, long position, long duration, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.slooshfilm.app.data.models.WatchHistoryWithDetails>> getWatchHistoryWithDetails() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object insertMovie(@org.jetbrains.annotations.NotNull()
    com.slooshfilm.app.data.model.Movie movie, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}