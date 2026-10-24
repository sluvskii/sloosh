package com.slooshfilm.app.data;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.coroutines.FlowUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.SQLiteConnection;
import androidx.sqlite.SQLiteStatement;
import com.slooshfilm.app.data.model.Movie;
import com.slooshfilm.app.data.models.WatchHistoryItem;
import com.slooshfilm.app.data.models.WatchHistoryWithDetails;
import java.lang.Class;
import java.lang.NullPointerException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class WatchHistoryDao_Impl implements WatchHistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<WatchHistoryItem> __insertAdapterOfWatchHistoryItem;

  public WatchHistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfWatchHistoryItem = new EntityInsertAdapter<WatchHistoryItem>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `watch_history` (`movieId`,`position`,`duration`,`watchedAt`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          @NonNull final WatchHistoryItem entity) {
        if (entity.getMovieId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getMovieId());
        }
        statement.bindLong(2, entity.getPosition());
        statement.bindLong(3, entity.getDuration());
        statement.bindLong(4, entity.getWatchedAt());
      }
    };
  }

  @Override
  public Object insert(final WatchHistoryItem item, final Continuation<? super Unit> $completion) {
    if (item == null) throw new NullPointerException();
    return DBUtil.performSuspending(__db, false, true, (_connection) -> {
      __insertAdapterOfWatchHistoryItem.insert(_connection, item);
      return Unit.INSTANCE;
    }, $completion);
  }

  @Override
  public Object getByMovieId(final String movieId,
      final Continuation<? super WatchHistoryItem> $completion) {
    final String _sql = "SELECT * FROM watch_history WHERE movieId = ?";
    return DBUtil.performSuspending(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (movieId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, movieId);
        }
        final int _columnIndexOfMovieId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "movieId");
        final int _columnIndexOfPosition = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "position");
        final int _columnIndexOfDuration = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "duration");
        final int _columnIndexOfWatchedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "watchedAt");
        final WatchHistoryItem _result;
        if (_stmt.step()) {
          final String _tmpMovieId;
          if (_stmt.isNull(_columnIndexOfMovieId)) {
            _tmpMovieId = null;
          } else {
            _tmpMovieId = _stmt.getText(_columnIndexOfMovieId);
          }
          final long _tmpPosition;
          _tmpPosition = _stmt.getLong(_columnIndexOfPosition);
          final long _tmpDuration;
          _tmpDuration = _stmt.getLong(_columnIndexOfDuration);
          final long _tmpWatchedAt;
          _tmpWatchedAt = _stmt.getLong(_columnIndexOfWatchedAt);
          _result = new WatchHistoryItem(_tmpMovieId,_tmpPosition,_tmpDuration,_tmpWatchedAt);
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
  public Flow<List<WatchHistoryWithDetails>> getWatchHistoryWithDetails() {
    final String _sql = "SELECT * FROM watch_history ORDER BY watchedAt DESC";
    return FlowUtil.createFlow(__db, true, new String[] {"movies",
        "watch_history"}, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfMovieId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "movieId");
        final int _columnIndexOfPosition = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "position");
        final int _columnIndexOfDuration = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "duration");
        final int _columnIndexOfWatchedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "watchedAt");
        final ArrayMap<String, Movie> _collectionMovie = new ArrayMap<String, Movie>();
        while (_stmt.step()) {
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfMovieId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfMovieId);
          }
          if (_tmpKey != null) {
            _collectionMovie.put(_tmpKey, null);
          }
        }
        _stmt.reset();
        __fetchRelationshipmoviesAscomSlooshfilmAppDataModelMovie(_connection, _collectionMovie);
        final List<WatchHistoryWithDetails> _result = new ArrayList<WatchHistoryWithDetails>();
        while (_stmt.step()) {
          final WatchHistoryWithDetails _item;
          final WatchHistoryItem _tmpWatchHistory;
          final String _tmpMovieId;
          if (_stmt.isNull(_columnIndexOfMovieId)) {
            _tmpMovieId = null;
          } else {
            _tmpMovieId = _stmt.getText(_columnIndexOfMovieId);
          }
          final long _tmpPosition;
          _tmpPosition = _stmt.getLong(_columnIndexOfPosition);
          final long _tmpDuration;
          _tmpDuration = _stmt.getLong(_columnIndexOfDuration);
          final long _tmpWatchedAt;
          _tmpWatchedAt = _stmt.getLong(_columnIndexOfWatchedAt);
          _tmpWatchHistory = new WatchHistoryItem(_tmpMovieId,_tmpPosition,_tmpDuration,_tmpWatchedAt);
          final Movie _tmpMovie;
          final String _tmpKey_1;
          if (_stmt.isNull(_columnIndexOfMovieId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _stmt.getText(_columnIndexOfMovieId);
          }
          if (_tmpKey_1 != null) {
            _tmpMovie = _collectionMovie.get(_tmpKey_1);
          } else {
            _tmpMovie = null;
          }
          _item = new WatchHistoryWithDetails(_tmpWatchHistory,_tmpMovie);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private void __fetchRelationshipmoviesAscomSlooshfilmAppDataModelMovie(
      @NonNull final SQLiteConnection _connection, @NonNull final ArrayMap<String, Movie> _map) {
    final Set<String> __mapKeySet = _map.keySet();
    if (__mapKeySet.isEmpty()) {
      return;
    }
    if (_map.size() > 999) {
      RelationUtil.recursiveFetchArrayMap(_map, false, (_tmpMap) -> {
        __fetchRelationshipmoviesAscomSlooshfilmAppDataModelMovie(_connection, _tmpMap);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("SELECT `url`,`title`,`imageUrl`,`isSeries`,`rating`,`year`,`filmName`,`filmYear`,`postId`,`type` FROM `movies` WHERE `url` IN (");
    final int _inputSize = __mapKeySet == null ? 1 : __mapKeySet.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final SQLiteStatement _stmt = _connection.prepare(_sql);
    int _argIndex = 1;
    if (__mapKeySet == null) {
      _stmt.bindNull(_argIndex);
    } else {
      for (String _item : __mapKeySet) {
        if (_item == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, _item);
        }
        _argIndex++;
      }
    }
    try {
      final int _itemKeyIndex = SQLiteStatementUtil.getColumnIndex(_stmt, "url");
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _columnIndexOfUrl = 0;
      final int _columnIndexOfTitle = 1;
      final int _columnIndexOfImageUrl = 2;
      final int _columnIndexOfIsSeries = 3;
      final int _columnIndexOfRating = 4;
      final int _columnIndexOfYear = 5;
      final int _columnIndexOfFilmName = 6;
      final int _columnIndexOfFilmYear = 7;
      final int _columnIndexOfPostId = 8;
      final int _columnIndexOfType = 9;
      while (_stmt.step()) {
        final String _tmpKey;
        if (_stmt.isNull(_itemKeyIndex)) {
          _tmpKey = null;
        } else {
          _tmpKey = _stmt.getText(_itemKeyIndex);
        }
        if (_tmpKey != null) {
          if (_map.containsKey(_tmpKey)) {
            final Movie _item_1;
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
            _item_1 = new Movie(_tmpUrl,_tmpTitle,_tmpImageUrl,_tmpIsSeries,_tmpRating,_tmpYear,_tmpFilmName,_tmpFilmYear,_tmpPostId,_tmpType);
            _map.put(_tmpKey, _item_1);
          }
        }
      }
    } finally {
      _stmt.close();
    }
  }
}
