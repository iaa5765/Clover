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
package org.floens.flutter.core.http;

import android.os.NetworkOnMainThreadException;
import android.text.TextUtils;

import com.android.volley.AuthFailureError;

import org.floens.flutter.Chan;
import org.floens.flutter.chan.ChanLoader;
import org.floens.flutter.chan.ChanUrls;
import org.floens.flutter.core.model.Loadable;
import org.floens.flutter.core.model.Reply;
import org.floens.flutter.core.net.ChanReaderRequest;
import org.floens.flutter.core.settings.ChanSettings;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.OkHttpClient;
import okhttp3.Call;

public class ReplyHttpCall extends HttpCall {
    private OkHttpClient httpClient;
    private static String hash ="";
    private static final String TAG = "ReplyHttpCall";
    private static final Random RANDOM = new Random();
    private static final Pattern THREAD_NO_PATTERN = Pattern.compile("<!-- thread:([0-9]+),no:([0-9]+) -->");
    private static final Pattern ERROR_MESSAGE = Pattern.compile("\"errmsg\"[^>]*>(.*?)<\\/span");

    public boolean posted;
    public String errorMessage;
    public String text;
    public String password;
    public int threadNo = -1;
    public int postNo = -1;

    private final Reply reply;

    public ReplyHttpCall(Reply reply) {
        this.reply = reply;
    }

    public static void setHash(String hash) {
        ReplyHttpCall.hash = hash;
    }

    @Override
    public Reply setup(Request.Builder requestBuilder) {
        String chan = Chan.getBoardManager().getBoardByCode(reply.board).chan;
        String board;
        if (reply.board.contains("1"))
            board = reply.board.substring(0, reply.board.length()-1);
        else
            board = reply.board;

        boolean thread = reply.resto >= 0;

        password = Long.toHexString(RANDOM.nextLong());

        MultipartBody.Builder formBuilder = new MultipartBody.Builder();
        formBuilder.setType(MultipartBody.FORM);

        formBuilder.addFormDataPart("name", reply.name);
        formBuilder.addFormDataPart("email", reply.options);

        if (chan.equals("ponyville") && hash.equals("")) {
            requestBuilder.url(ChanUrls.getThreadUrlDesktop(board, reply.resto, chan));
            //requestBuilder.post(formBuilder.build());
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10000, TimeUnit.MILLISECONDS)
                    .readTimeout(10000, TimeUnit.MILLISECONDS)
                    .writeTimeout(10000, TimeUnit.MILLISECONDS)
                    // Disable SPDY, causes reproducible timeouts, only one download at the same time and other fun stuff
                    .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                    .build();

            requestBuilder.tag("HttpRequest");

            System.out.println(hash);

            Call call = client.newCall(requestBuilder.build());
            call.enqueue(new ReplyHttpCall(reply));


            //requestBuilder.post(formBuilder.build());
            System.out.println(call.request().toString());
            String responseBody = "";

            Response response = null;

            try {
                try {
                    try {
                        response = call.execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    responseBody = response.body().toString();
                } catch (NetworkOnMainThreadException e) {
                }
            } catch (IllegalStateException e) {
            }

            System.out.println("response: " + responseBody);

        }

        if (!thread && !TextUtils.isEmpty(reply.subject)) {
            formBuilder.addFormDataPart("subject", reply.subject);
        } else
            formBuilder.addFormDataPart("subject", "");

        formBuilder.addFormDataPart("body", reply.comment);
        if (reply.file != null) {
            formBuilder.addFormDataPart("file", reply.fileName, RequestBody.create(
                    MediaType.parse("application/octet-stream"), reply.file
            ));
        }
        formBuilder.addFormDataPart("password", password);
        if (thread) {
            formBuilder.addFormDataPart("thread", String.valueOf(reply.resto));
        }
        formBuilder.addFormDataPart("board", board);

        if (chan.equals("ponychan")) {
            formBuilder.addFormDataPart("making_a_post", "1");
            formBuilder.addFormDataPart("post", "New Reply");
            formBuilder.addFormDataPart("wantjson", "1");
        } else {
            formBuilder.addFormDataPart("post", "Post");
            formBuilder.addFormDataPart("hash", this.hash);
            formBuilder.addFormDataPart("json_response", "1");
        }

         if (thread) {
             formBuilder.addFormDataPart("resto", String.valueOf(reply.resto));
         }

        if (reply.spoilerImage) {
            formBuilder.addFormDataPart("spoiler", "on");
        }

        requestBuilder.url(ChanUrls.getReplyUrl(chan));
        System.out.println(hash);
        requestBuilder.post(formBuilder.build());

        this.posted = true;
        return reply;


    }

    @Override
    public void process(Response response, String result) throws IOException {
        text = result;

        Matcher errorMessageMatcher = ERROR_MESSAGE.matcher(result);
        if (errorMessageMatcher.find()) {
            errorMessage = Jsoup.parse(errorMessageMatcher.group(1)).body().text();
        } else {
            Matcher threadNoMatcher = THREAD_NO_PATTERN.matcher(result);
            if (threadNoMatcher.find()) {
                try {
                    threadNo = Integer.parseInt(threadNoMatcher.group(1));
                    postNo = Integer.parseInt(threadNoMatcher.group(2));
                } catch (NumberFormatException ignored) {
                }

                if (threadNo >= 0 && postNo >= 0) {
                    posted = true;
                }
            }
        }
    }
}
