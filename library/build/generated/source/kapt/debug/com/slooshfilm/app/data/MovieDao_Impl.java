package com.slooshfilm.app.data;

import androidx.annotation.NonNull;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import com.slooshfilm.app.data.model.Movie;
import java.lang.Class;
import java.lang.NullPointerException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class MovieDao_Impl implements MovieDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<Movie> __insertAdapterOfMovie;

  public MovieDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfMovie = new EntityInsertAdapter<Movie>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `movies` (`url`,`title`,`imageUrl`,`isSeries`,`rating`,`year`,`filmName`,`filmYear`,`postId`,`type`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, @NonNull final Movie entity) {
        if (entity.getUrl() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getUrl());
        }
        if (entity.getTitle() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getTitle());
        }
        if (entity.getImageUrl() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getImageUrl());
        }
        final int _tmp = entity.isSeries() ? 1 : 0;
        statement.bindLong(4, _tmp);
        if (entity.getRating() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getRating());
        }
        if (entity.getYear() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getYear());
        }
        if (entity.getFilmName() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getFilmName());
        }
        if (entity.getFilmYear() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getFilmYear());
        }
        if (entity.getPostId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindText(9, entity.getPostId());
        }
        if (entity.getType() == null) {
          statement.bindNull(10);
        } else {
          statement.bindText(10, entity.getType());
        }
      }
    };
  }

  @Override
  public Object insertMovie(final Movie movie, final Continuation<? super Unit> $completion) {
    if (movie == null) throw new NullPointerException();
    return DBUtil.performSuspending(__db, false, true, (_connection) -> {
      __insertAdapterOfMovie.insert(_connection, movie);
      return Unit.INSTANCE;
    }, $completion);
  }

  @Override
  public Object getMovie(final String url, final Continuation<? super Movie> $completion) {
    final String _sql = "SELECT * FROM movies WHERE url = ?";
    return DBUtil.performSuspending(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (url == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, url);
        }
        final int _columnIndexOfUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "url");
        final int _columnIndexOfTitle = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "title");
        final int _columnIndexOfImageUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "imageUrl");
        final int _columnIndexOfIsSeries = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "isSeries");
        final int _columnIndexOfRating = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "rating");
        final int _columnIndexOfYear = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "year");
        final int _columnIndexOfFilmName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "filmName");
        final int _columnIndexOfFilmYear = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "filmYear");
        final int _columnIndexOfPostId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "postId");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final Movie _result;
        if (_stmt.step()) {
          final String _tmpUrl;
          if (_stmt.isNull(_columnIndexOfUrl)) {
            _tmpUrl = null;
          } else {
            _tmpUrl = _stmt.getText(_columnIndexOfUrl);
          }
          final String _tmpTitle;
          if (_stmt.isNull(_columnIndexOfTitle)) {
            _tmpTitle = null;
          } else {
            _tmpTitle = _stmt.getText(_columnIndexOfTitle);
          }
          final String _tmpImageUrl;
          if (_stmt.isNull(_columnIndexOfImageUrl)) {
            _tmpImageUrl = null;
          } else {
            _tmpImageUrl = _stmt.getText(_columnIndexOfImageUrl);
          }
          final boolean _tmpIsSeries;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfIsSeries));
          _tmpIsSeries = _tmp != 0;
          final String _tmpRating;
          if (_stmt.isNull(_columnIndexOfRating)) {
            _tmpRating = null;
          } else {
            _tmpRating = _stmt.getText(_columnIndexOfRating);
          }
          final String _tmpYear;
          if (_stmt.isNull(_columnIndexOfYear)) {
            _tmpYear = null;
          } else {
            _tmpYear = _stmt.getText(_columnIndexOfYear);
          }
          final String _tmpFilmName;
          if (_stmt.isNull(_columnIndexOfFilmName)) {
            _tmpFilmName = null;
          } else {
            _tmpFilmName = _stmt.getText(_columnIndexOfFilmName);
          }
          final String _tmpFilmYear;
          if (_stmt.isNull(_columnIndexOfFilmYear)) {
            _tmpFilmYear = null;
          } else {
            _tmpFilmYear = _stmt.getText(_columnIndexOfFilmYear);
          }
          final String _tmpPostId;
          if (_stmt.isNull(_columnIndexOfPostId)) {
            _tmpPostId = null;
          } else {
            _tmpPostId = _stmt.getText(_columnIndexOfPostId);
          }
          final String _tmpType;
          if (_stmt.isNull(_columnIndexOfType)) {
            _tmpType = null;
          } else {
            _tmpType = _stmt.getText(_columnIndexOfType);
          }
          _result = new Movie(_tmpUrl,_tmpTitle,_tmpImageUrl,_tmpIsSeries,_tmpRating,_tmpYear,_tmpFilmName,_tmpFilmYear,_tmpPostId,_tmpType);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @Override
  public Object getAllMovies(final Continuation<? super List<Movie>> $completion) {
    final String _sql = "SELECT * FROM movies";
    return DBUtil.performSuspending(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "url");
        final int _columnIndexOfTitle = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "title");
        final int _columnIndexOfImageUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "imageUrl");
        final int _columnIndexOfIsSeries = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "isSeries");
        final int _columnIndexOfRating = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "rating");
        final int _columnIndexOfYear = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "year");
        final int _columnIndexOfFilmName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "filmName");
        final int _columnIndexOfFilmYear = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "filmYear");
        final int _columnIndexOfPostId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "postId");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final List<Movie> _result = new ArrayList<Movie>();
        while (_stmt.step()) {
          final Movie _item;
          final String _tmpUrl;
          if (_stmt.isNull(_columnIndexOfUrl)) {
            _tmpUrl = null;
          } else {
            _tmpUrl = _stmt.getText(_columnIndexOfUrl);
          }
          final String _tmpTitle;
          if (_stmt.isNull(_columnIndexOfTitle)) {
            _tmpTitle = null;
          } else {
            _tmpTitle = _stmt.getText(_columnIndexOfTitle);
          }
          final String _tmpImageUrl;
          if (_stmt.isNull(_columnIndexOfImageUrl)) {
            _tmpImageUrl = null;
          } else {
            _tmpImageUrl = _stmt.getText(_columnIndexOfImageUrl);
          }
          final boolean _tmpIsSeries;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfIsSeries));
          _tmpIsSeries = _tmp != 0;
          final String _tmpRating;
          if (_stmt.isNull(_columnIndexOfRating)) {
            _tmpRating = null;
          } else {
            _tmpRating = _stmt.getText(_columnIndexOfRating);
          }
          final String _tmpYear;
          if (_stmt.isNull(_columnIndexOfYear)) {
            _tmpYear = null;
          } else {
            _tmpYear = _stmt.getText(_columnIndexOfYear);
          }
          final String _tmpFilmName;
          if (_stmt.isNull(_columnIndexOfFilmName)) {
            _tmpFilmName = null;
          } else {
            _tmpFilmName = _stmt.getText(_columnIndexOfFilmName);
          }
          final String _tmpFilmYear;
          if (_stmt.isNull(_columnIndexOfFilmYear)) {
            _tmpFilmYear = null;
          } else {
            _tmpFilmYear = _stmt.getText(_columnIndexOfFilmYear);
          }
          final String _tmpPostId;
          if (_stmt.isNull(_columnIndexOfPostId)) {
            _tmpPostId = null;
          } else {
            _tmpPostId = _stmt.getText(_columnIndexOfPostId);
          }
          final String _tmpType;
          if (_stmt.isNull(_columnIndexOfType)) {
            _tmpType = null;
          } else {
            _tmpType = _stmt.getText(_columnIndexOfType);
          }
          _item = new Movie(_tmpUrl,_tmpTitle,_tmpImageUrl,_tmpIsSeries,_tmpRating,_tmpYear,_tmpFilmName,_tmpFilmYear,_tmpPostId,_tmpType);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @Override
  public Object deleteMovie(final String url, final Continuation<? super Unit> $completion) {
    final String _sql = "DELETE FROM movies WHERE url = ?";
    return DBUtil.performSuspending(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (url == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, url);
        }
        _stmt.step();
        return Unit.INSTANCE;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
