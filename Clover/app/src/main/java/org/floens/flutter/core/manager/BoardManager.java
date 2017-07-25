/*
 * Clover - 4chan browser https://github.com/Floens/Clover/
 * Copyright (C) 2014  Floens
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.floens.flutter.core.manager;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.floens.flutter.Chan;
import org.floens.flutter.chan.ChanUrls;
import org.floens.flutter.core.database.DatabaseManager;
import org.floens.flutter.core.model.Board;
import org.floens.flutter.core.net.BoardsRequest;
import org.floens.flutter.utils.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

public class BoardManager implements Response.Listener<List<Board>>, Response.ErrorListener {
    private static final String TAG = "BoardManager";

    private static final Comparator<Board> ORDER_SORT = new Comparator<Board>() {
        @Override
        public int compare(Board lhs, Board rhs) {
            return lhs.order - rhs.order;
        }
    };

    private static final Comparator<Board> NAME_SORT = new Comparator<Board>() {
        @Override
        public int compare(Board lhs, Board rhs) {
            return lhs.name.compareTo(rhs.name);
        }
    };

    private final DatabaseManager databaseManager;

    private final List<Board> boards;
    private final List<Board> savedBoards = new ArrayList<>();
    private final Map<String, Board> boardsByCode = new HashMap<>();

    public BoardManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        boards = databaseManager.getBoards();

        if (boards.isEmpty()) {
            Logger.d(TAG, "Loading default boards");
            boards.addAll(getDefaultBoards());
            saveDatabase();
            update(true);
        } else {
            update(false);
        }

        //Chan.getVolleyRequestQueue().add(new BoardsRequest(ChanUrls.getBoardsUrl(), this, this));
    }

    @Override
    public void onResponse(List<Board> response) {
        List<Board> boardsToAddWs = new ArrayList<>();
        List<Board> boardsToAddNws = new ArrayList<>();

        for (int i = 0; i < response.size(); i++) {
            Board serverBoard = response.get(i);

            Board existing = getBoardByCode(serverBoard.code);
            if (existing != null) {
                serverBoard.id = existing.id;
                serverBoard.saved = existing.saved;
                serverBoard.order = existing.order;
                boards.set(boards.indexOf(existing), serverBoard);
            } else {
                serverBoard.saved = true;
                if (serverBoard.workSafe) {
                    boardsToAddWs.add(serverBoard);
                } else {
                    boardsToAddNws.add(serverBoard);
                }
            }
        }

        Collections.sort(boardsToAddWs, NAME_SORT);
        Collections.sort(boardsToAddNws, NAME_SORT);

        for (int i = 0; i < boardsToAddWs.size(); i++) {
            Board board = boardsToAddWs.get(i);
            board.order = boards.size();
            boards.add(board);
        }

        for (int i = 0; i < boardsToAddNws.size(); i++) {
            Board board = boardsToAddNws.get(i);
            board.order = boards.size();
            boards.add(board);
        }

        saveDatabase();
        update(true);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Logger.e(TAG, "Failed to get boards from server");
    }

    // Thread-safe
    public boolean getBoardExists(String code) {
        return getBoardByCode(code) != null;
    }

    // Thread-safe
    public Board getBoardByCode(String code) {
        synchronized (boardsByCode) {
            return boardsByCode.get(code);
        }
    }

    public List<Board> getAllBoards() {
        return boards;
    }

    public List<Board> getSavedBoards() {
        return savedBoards;
    }

    public void flushOrderAndSaved() {
        saveDatabase();
        update(true);
    }

    private void update(boolean notify) {
        savedBoards.clear();
        savedBoards.addAll(filterSaved(boards));
        synchronized (boardsByCode) {
            boardsByCode.clear();
            for (Board board : boards) {
                boardsByCode.put(board.code, board);
            }
        }
        if (notify) {
            EventBus.getDefault().post(new BoardsChangedMessage());
        }
    }

    private void saveDatabase() {
        databaseManager.setBoards(boards);
    }

    private List<Board> getDefaultBoards() {
        List<Board> list = new ArrayList<>();
        list.add(new Board("(PC) Oatmeal", "oat", true, true, "ponychan"));
        list.add(new Board("(PC) Show Discussion", "pony", true, true, "ponychan"));
        list.add(new Board("(PC) Chat", "chat", true, true, "ponychan"));
        list.add(new Board("(PC) Roleplay", "rp", true, true, "ponychan"));
        list.add(new Board("(PC) Fanworks", "fan", true, true, "ponychan"));
        list.add(new Board("(PC) Site Issues", "site", true, true, "ponychan"));
        list.add(new Board("(PC) Twilight's Library", "arch", true, true, "ponychan"));
        list.add(new Board("(PC) The Dungeon", "moon", false, true, "ponychan"));
        list.add(new Board("(PC) Art", "art", false, true, "ponychan"));
        list.add(new Board("(PC) Projects", "collab", false, true, "ponychan"));
        list.add(new Board("(PC) Pictures", "pic", false, true, "ponychan"));
        list.add(new Board("(PC) Fanfiction", "fic", false, true, "ponychan"));
        list.add(new Board("(PC) Games", "g", false, true, "ponychan"));
        list.add(new Board("(PC) World", "int", false, true, "ponychan"));
        list.add(new Board("(PC) Merchandise", "merch", false, true, "ponychan"));
        list.add(new Board("(PC) Suggestion Box", "meta", false, true, "ponychan"));
        list.add(new Board("(PC) Roleplay Lounge", "ooc", false, true, "ponychan"));
        list.add(new Board("(PC) Deletion Board", "trash", false, true, "ponychan"));
        list.add(new Board("(PC) Music", "vinyl", false, true, "ponychan"));
        list.add(new Board("(PV) Pony", "pony1", true, true, "ponyville"));
        list.add(new Board("(PV) Cartoon", "cartoon", false, true, "ponyville"));
        list.add(new Board("(PV) Roleplay", "rp1", true, true, "ponyville"));
        list.add(new Board("(PV) Canterlot", "canterlot", true, true, "ponyville"));
        list.add(new Board("(PV) Ponyville Municipal Archives", "arch1", true, true, "ponyville"));
        list.add(new Board("(PV) Testing", "test1", false, true, "ponyville"));

        return list;
    }

    private List<Board> filterSaved(List<Board> all) {
        List<Board> saved = new ArrayList<>(all.size());
        for (int i = 0; i < all.size(); i++) {
            Board board = all.get(i);
            if (board.saved) {
                saved.add(board);
            }
        }
        Collections.sort(saved, ORDER_SORT);
        return saved;
    }

    public static class BoardsChangedMessage {
    }
}
