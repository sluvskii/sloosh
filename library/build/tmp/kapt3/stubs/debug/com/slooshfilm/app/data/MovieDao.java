package com.slooshfilm.app.data;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0003\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u0007\u001a\u0004\u0018\u00010\u00052\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00050\fH\u00a7@\u00a2\u0006\u0002\u0010\rJ\u0016\u0010\u000e\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\n\u00a8\u0006\u000f\u00c0\u0006\u0003"}, d2 = {"Lcom/slooshfilm/app/data/MovieDao;", "", "insertMovie", "", "movie", "Lcom/slooshfilm/app/data/model/Movie;", "(Lcom/slooshfilm/app/data/model/Movie;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getMovie", "url", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllMovies", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteMovie", "library_debug"})
@androidx.room.Dao()
public abstract interface MovieDao {
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertMovie(@org.jetbrains.annotations.NotNull()
    com.slooshfilm.app.data.model.Movie movie, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM movies WHERE url = :url")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMovie(@org.jetbrains.annotations.NotNull()
    java.lang.String url, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.slooshfilm.app.data.model.Movie> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM movies")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAllMovies(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.slooshfilm.app.data.model.Movie>> $completion);
    
    @androidx.room.Query(value = "DELETE FROM movies WHERE url = :url")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteMovie(@org.jetbrains.annotations.NotNull()
    java.lang.String url, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}