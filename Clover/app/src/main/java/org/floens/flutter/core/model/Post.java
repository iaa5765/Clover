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
package org.floens.flutter.core.model;

import android.text.SpannableString;
import android.text.TextUtils;

import org.floens.flutter.Chan;
import org.floens.flutter.chan.ChanParser;
import org.floens.flutter.chan.ChanUrls;
import org.floens.flutter.core.settings.ChanSettings;
import org.jsoup.parser.Parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains all data needed to represent a single post.<br>
 * Call {@link #finish()} to parse the comment etc. The post data is invalid if finish returns false.<br>
 * This class has members that are threadsafe and some that are not, see the source for more info.
 */
public class Post {
    private static final Random random = new Random();

    // *** These next members don't get changed after finish() is called. Effectively final. ***
    public String board;

    public int no = -1;

    public int resto = -1;

    public boolean isOP = false;

    public String date;

    public String name = "";

    public CharSequence comment = "";

    public String subject = "";

    public long tim = -1;

    public String ext;

    public String file;

    public int imageWidth;

    public int imageHeight;

    public boolean hasImage = false;

    public PostImage image;

    public String thumbnailUrl;

    public String imageUrl;

    public String tripcode = "";

    public String id = "";

    public String capcode = "";

    public String country = "";

    public String countryName = "";

    public String trollCountry = "";

    public long time = -1;

    public long fileSize;

    public String rawComment;

    private String preRawComment;

    public String countryUrl;

    public boolean spoiler = false;

    public boolean isSavedReply = false;

    public int filterHighlightedColor = 0;

    public boolean filterStub = false;

    public boolean filterRemove = false;


    /**
     * This post replies to the these ids. Is an unmodifiable set after finish().
     */
    public Set<Integer> repliesTo = new TreeSet<>();

    public final ArrayList<PostLinkable> linkables = new ArrayList<>();

    public SpannableString subjectSpan;

    public SpannableString nameSpan;

    public SpannableString tripcodeSpan;

    public SpannableString idSpan;

    public SpannableString capcodeSpan;

    public CharSequence nameTripcodeIdCapcodeSpan;

    // *** These next members may only change on the main thread after finish(). ***
    public boolean sticky = false;
    public boolean closed = false;
    public boolean archived = false;
    public int replies = -1;
    public int images = -1;
    public int uniqueIps = 1;
    public String title = "";

    // *** Threadsafe members, may be read and modified on any thread. ***
    public AtomicBoolean deleted = new AtomicBoolean(false);

    // *** Manual synchronization needed. ***
    /**
     * These ids replied to this post.<br>
     * <b>synchronize on this when accessing.</b>
     */
    public final List<Integer> repliesFrom = new ArrayList<>();

    /**
     * Parse and modify raw comment for formatting
     *
     * @return formatted comment
     */
    public String parseRawComment(String raw)     {
        //fix linebreaks first
        raw = raw.replaceAll("(\\r)?\\n", "<br>\n");

        //System.out.println(raw);

        //fix greentext
        {
            Pattern regex = Pattern.compile("(^(&gt;)((?!(&gt;))\\D).*)", Pattern.MULTILINE);
            Matcher regexMatcher = regex.matcher(raw);
            StringBuffer temp = new StringBuffer();
            boolean matchesFound = false;

            while (regexMatcher.find())
            {
                matchesFound = true;
                regexMatcher.appendReplacement(temp, "<span class=\"quote\">&gt;" + regexMatcher.group(1).toString().substring(4) + "</span><br>");
            }

            if (matchesFound)
                regexMatcher.appendTail(temp);
            else
                temp.append(raw);

            raw = temp.toString();
        }

        //fix orangetext
        {
            Pattern regex = Pattern.compile("(^(&lt;).*)", Pattern.MULTILINE);
            Matcher regexMatcher = regex.matcher(raw);
            StringBuffer temp = new StringBuffer();
            boolean matchesFound = false;

            while (regexMatcher.find())
            {
                matchesFound = true;
                regexMatcher.appendReplacement(temp, "<span class=\"orangequote\" style=\"color: #E07000\">&lt;" + regexMatcher.group(1).toString().substring(4) + "</span><br>");
            }

            if (matchesFound)
                regexMatcher.appendTail(temp);
            else
                temp.append(raw);

            raw = temp.toString();
        }

        //fix post links
        {
            Pattern regex = Pattern.compile("(&gt;&gt;\\d+)");
            Matcher regexMatcher = regex.matcher(raw);
            StringBuffer temp = new StringBuffer();
            boolean matchesFound = false;

            while (regexMatcher.find())
            {
                matchesFound = true;
                regexMatcher.appendReplacement(temp, "<a href=\"#p" + regexMatcher.group(1).toString().substring(8) + "\" class=\"quotelink\">&gt;&gt;" + regexMatcher.group(1).toString().substring(8) + "</a>");
            }

            if (matchesFound)
                regexMatcher.appendTail(temp);
            else
                temp.append(raw);

            raw = temp.toString();
        }

        //url-bbcode
        {
            Pattern regex = Pattern.compile("\\[url=(?:&quot;)?(.+?)(?:&quot;)?\\](.+?)\\[/url\\]");
            Matcher regexMatcher = regex.matcher(raw);
            StringBuffer temp = new StringBuffer();
            boolean matchesFound = false;

            while (regexMatcher.find())
            {
                matchesFound = true;
                regexMatcher.appendReplacement(temp, "<a href=\"" + regexMatcher.group(1).toString() + "\">" + regexMatcher.group(2).toString() + "</a>");
            }

            if (matchesFound)
                regexMatcher.appendTail(temp);
            else
                temp.append(raw);

            raw = temp.toString();
        }

        //spoilers
        {
            Pattern regex = Pattern.compile("(\\[\\?](.+?)\\[/\\?]|\\[spoiler](.+?)\\[/spoiler])");
            Matcher regexMatcher = regex.matcher(raw);
            StringBuffer temp = new StringBuffer();
            boolean matchesFound = false;

            while (regexMatcher.find())
            {
                matchesFound = true;
                regexMatcher.appendReplacement(temp, "<s>" + regexMatcher.group(1).toString() + "</s>");
            }

            if (matchesFound)
                regexMatcher.appendTail(temp);
            else
                temp.append(raw);

            raw = temp.toString();
        }

        //bold
        {
            Pattern regex = Pattern.compile("\\[b](.+?)\\[/b]");
            Matcher regexMatcher = regex.matcher(raw);
            StringBuffer temp = new StringBuffer();
            boolean matchesFound = false;

            while (regexMatcher.find())
            {
                matchesFound = true;
                regexMatcher.appendReplacement(temp, "<b>" + regexMatcher.group(1).toString() + "</b>");
            }

            if (matchesFound)
                regexMatcher.appendTail(temp);
            else
                temp.append(raw);

            raw = temp.toString();
        }

        //italics
        {
            Pattern regex = Pattern.compile("\\[i](.+?)\\[/i]");
            Matcher regexMatcher = regex.matcher(raw);
            StringBuffer temp = new StringBuffer();
            boolean matchesFound = false;

            while (regexMatcher.find())
            {
                matchesFound = true;
                regexMatcher.appendReplacement(temp, "<i>" + regexMatcher.group(1).toString() + "</i>");
            }

            if (matchesFound)
                regexMatcher.appendTail(temp);
            else
                temp.append(raw);

            raw = temp.toString();
        }

        //underline
        {
            Pattern regex = Pattern.compile("\\[u](.+?)\\[/u]");
            Matcher regexMatcher = regex.matcher(raw);
            StringBuffer temp = new StringBuffer();
            boolean matchesFound = false;

            while (regexMatcher.find())
            {
                matchesFound = true;
                regexMatcher.appendReplacement(temp, "<u>" + regexMatcher.group(1).toString() + "</u>");
            }

            if (matchesFound)
                regexMatcher.appendTail(temp);
            else
                temp.append(raw);

            raw = temp.toString();
        }

        //strikethrough
        {
            Pattern regex = Pattern.compile("\\[s](.+?)\\[/s]");
            Matcher regexMatcher = regex.matcher(raw);
            StringBuffer temp = new StringBuffer();
            boolean matchesFound = false;

            while (regexMatcher.find())
            {
                matchesFound = true;
                regexMatcher.appendReplacement(temp, "<span class=\"strikethrough\">" + regexMatcher.group(1).toString() + "</span>");
            }

            if (matchesFound)
                regexMatcher.appendTail(temp);
            else
                temp.append(raw);

            raw = temp.toString();
        }

        //shy
        {
            Pattern regex = Pattern.compile("\\[shy](.+?)\\[/shy]");
            Matcher regexMatcher = regex.matcher(raw);
            StringBuffer temp = new StringBuffer();
            boolean matchesFound = false;

            while (regexMatcher.find())
            {
                matchesFound = true;
                regexMatcher.appendReplacement(temp, "<sup>" + regexMatcher.group(1).toString() + "</sup>");
            }

            if (matchesFound)
                regexMatcher.appendTail(temp);
            else
                temp.append(raw);

            raw = temp.toString();
        }

        //shy
        {
            Pattern regex = Pattern.compile("\\[shy](.+?)\\[/shy]");
            Matcher regexMatcher = regex.matcher(raw);
            StringBuffer temp = new StringBuffer();
            boolean matchesFound = false;

            while (regexMatcher.find())
            {
                matchesFound = true;
                regexMatcher.appendReplacement(temp, "<sup>" + regexMatcher.group(1).toString() + "</sup>");
            }

            if (matchesFound)
                regexMatcher.appendTail(temp);
            else
                temp.append(raw);

            raw = temp.toString();
        }

        //rcv filter
        {
            Pattern regex = Pattern.compile("\\[rcv](.+?)\\[/rcv]");
            Matcher regexMatcher = regex.matcher(raw);
            StringBuffer temp = new StringBuffer();
            boolean matchesFound = false;

            while (regexMatcher.find())
            {
                matchesFound = true;
                regexMatcher.appendReplacement(temp, "<strong>" + regexMatcher.group(1).toString().toUpperCase() + "</strong>");
            }

            if (matchesFound)
                regexMatcher.appendTail(temp);
            else
                temp.append(raw);

            raw = temp.toString();
        }

        //red headers
        {
            Pattern regex = Pattern.compile("==(.+?)==");
            Matcher regexMatcher = regex.matcher(raw);
            StringBuffer temp = new StringBuffer();
            boolean matchesFound = false;

            while (regexMatcher.find())
            {
                matchesFound = true;
                regexMatcher.appendReplacement(temp, "<strong>" + regexMatcher.group(1).toString().toUpperCase() + "</strong>");
            }

            if (matchesFound)
                regexMatcher.appendTail(temp);
            else
                temp.append(raw);

            raw = temp.toString();
        }

        System.out.println("=========DEBUGGING RAW COMMENT===========");
        System.out.print(raw);
        System.out.println("=========DEBUGGING RAW COMMENT===========");

        return raw;
    }


    /**
     * Finish up the data: parse the comment, check if the data is valid etc.
     *
     * @return false if this data is invalid
     */
    public boolean finish() {
        String chan = Chan.getBoardManager().getBoardByCode(board).chan;

        preRawComment = rawComment;
        if (chan.equals("ponychan")) {
            rawComment = TextUtils.htmlEncode(rawComment);
            rawComment = parseRawComment(rawComment);
        }
        comment = rawComment;

        if (board == null || no < 0 || resto < 0 || /*date == null ||*/ time < 0) {
            return false;
        }

        isOP = resto == 0;

        if (isOP && (replies < 0 || images < 0)) {
            return false;
        }

        if (file != null && imageWidth > 0 && imageHeight > 0)  {
            hasImage = true;
            if (file.contains("mtr")) {
                tim = Long.parseLong(file.substring(4,17));
                imageUrl = ChanUrls.getImageUrl(board, "mtr_" + Long.toString(tim), ext, chan);
            } else {
                tim = Long.parseLong(file.substring(0,13));
                imageUrl = ChanUrls.getImageUrl(board, Long.toString(tim), ext, chan);
            }
            file = Parser.unescapeEntities(file, false);

            boolean spoilerImage = spoiler && !ChanSettings.revealImageSpoilers.get();
            if (spoilerImage) {
                Board b = Chan.getBoardManager().getBoardByCode(board);
                if (b != null && b.customSpoilers >= 0) {
                    thumbnailUrl = ChanUrls.getCustomSpoilerUrl(board, random.nextInt(b.customSpoilers) + 1);
                } else {
                    thumbnailUrl = ChanUrls.getSpoilerUrl();
                }
            } else {
                if (file.contains("mtr")) {
                    thumbnailUrl = ChanUrls.getThumbnailUrl(board, "mtr_" + Long.toString(tim), ext, chan);
                } else thumbnailUrl = ChanUrls.getThumbnailUrl(board, Long.toString(tim), ext, chan);
            }

            image = new PostImage(String.valueOf(tim), thumbnailUrl, imageUrl, Long.toString(tim), ext, imageWidth, imageHeight, spoilerImage, fileSize);
        }

        if (!TextUtils.isEmpty(countryName)) {
            if (!TextUtils.isEmpty(trollCountry)) {
                countryUrl = ChanUrls.getCountryTrollFlagUrl(trollCountry);
            } else {
                countryUrl = ChanUrls.getCountryFlagUrl(country);
            }
        }

        ChanParser.getInstance().parse(this);

        repliesTo = Collections.unmodifiableSet(repliesTo);

        return true;
    }
}
