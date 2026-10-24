package com.slooshfilm.app.data;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u0007\u001a\u0004\u0018\u00010\u00052\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0014\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\r0\fH\'\u00a8\u0006\u000f\u00c0\u0006\u0003"}, d2 = {"Lcom/slooshfilm/app/data/WatchHistoryDao;", "", "insert", "", "item", "Lcom/slooshfilm/app/data/models/WatchHistoryItem;", "(Lcom/slooshfilm/app/data/models/WatchHistoryItem;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getByMovieId", "movieId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getWatchHistoryWithDetails", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/slooshfilm/app/data/models/WatchHistoryWithDetails;", "library_debug"})
@androidx.room.Dao()
public abstract interface WatchHistoryDao {
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    com.slooshfilm.app.data.models.WatchHistoryItem item, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM watch_history WHERE movieId = :movieId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getByMovieId(@org.jetbrains.annotations.NotNull()
    java.lang.String movieId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.slooshfilm.app.data.models.WatchHistoryItem> $completion);
    
    @androidx.room.Transaction()
    @androidx.room.Query(value = "SELECT * FROM watch_history ORDER BY watchedAt DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.slooshfilm.app.data.models.WatchHistoryWithDetails>> getWatchHistoryWithDetails();
}