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
package org.floens.chan.chan;

import org.floens.chan.core.settings.ChanSettings;

import java.util.Locale;
import java.util.Objects;

public class ChanUrls {
    public static String getCatalogUrl(String board) {
        return scheme() + "://ponychan.net/api.php?req=catalog&board=" + board;
        //return scheme() + "://a.4cdn.org/qst/catalog.json";
    }

    public static String getPageUrl(String board, int pageNumber) {
        return scheme() + "://ponychan.net/api.php?req=threads&board=" + board + "&page=" + (pageNumber + 1);
    }

    public static String getThreadUrl(String board, int no) {
        //return scheme() + "://ponychan.net/api.php?req=thread&board=" + board + "&thread=2";
        return scheme() + "://ponychan.net/api.php?req=thread&board=" + board + "&thread=" + no;
    }

    public static String getCaptchaSiteKey() {
        return "6Ldp2bsSAAAAAAJ5uyx_lx34lJeEpTLVkP5k04qc";
    }

    public static String getImageUrl(String board, String code, String extension) {
        return scheme() + "://ponychan.net/" + board + "/src/" + code + "." + extension;

        //return scheme() + "://ponychan.net/" + board + "/src/" + code + ".jpg";

    }

    public static String getThumbnailUrl(String board, String code, String ext) {
        if (Objects.equals(ext, "webm")) {
            return "://ponychan.net/" + board + "/thumb/" + code + ".jpg";
        } else if (Objects.equals(code, "1420222744903")) {
            return scheme() + "://ml.ponychan.net/" + board + "/thumb/" + code + "." + ext;
        } else
        return scheme() + "://ponychan.net/" + board + "/thumb/" + code + "." + ext;

    }

    public static String getSpoilerUrl() {
        return scheme() + "://www.ponychan.net/static/spoiler.png";
    }

    public static String getCustomSpoilerUrl(String board, int value) {
        return scheme() + "://www.ponychan.net/static/spoiler.png";
    }

    public static String getCountryFlagUrl(String countryCode) {
        return scheme() + "://s.4cdn.org/image/country/" + countryCode.toLowerCase(Locale.ENGLISH) + ".gif";
    }

    public static String getCountryTrollFlagUrl(String trollCode) {
        return scheme() + "://s.4cdn.org/image/country/troll/" + trollCode.toLowerCase(Locale.ENGLISH) + ".gif";
    }

    public static String getBoardsUrl() {
        return scheme() + "://ponychan.net/api.php?req=boards";
    }

    public static String getReplyUrl(String board) {
        return scheme() + "://www.ponychan.net/post.php";
    }

    public static String getDeleteUrl(String board) {
        return scheme() + "://www.ponychan.net/post.php";
    }

    public static String getBoardUrlDesktop(String board) {
        return scheme() + "://ponychan.net/" + board + "/";
    }

    public static String getThreadUrlDesktop(String board, int no) {
        return scheme() + "://ponychan.net/" + board + "/res/" + no + ".html";
    }

    public static String getThreadUrlDesktop(String board, int no, int postNo) {
        return scheme() + "://ponychan.net/" + board + "/res/" + no + ".html#" + postNo;
    }

    public static String getCatalogUrlDesktop(String board) {
        return scheme() + "://ponychan.net/" + board + "/catalog.html";
    }

    public static String getPassUrl() {
        return "https://sys.4chan.org/auth";
    }

    public static String getReportDomain() {
        return "https://sys.4chan.org/";
    }

    public static String[] getReportCookies(String passId) {
        return new String[]{"pass_enabled=1;", "pass_id=" + passId + ";"};
    }

    public static String getReportUrl(String board, int no) {
        return "https://sys.4chan.org/" + board + "/imgboard.php?mode=report&no=" + no;
    }

    private static String scheme() {
        return ChanSettings.networkHttps.get() ? "https" : "http";
    }
}
