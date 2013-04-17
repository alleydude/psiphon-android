/*
 * Copyright (c) 2013, Psiphon Inc.
 * All rights reserved.
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
 *
 */

package com.psiphon3;

import java.util.List;

import com.psiphon3.FeedbackActivity;
import com.psiphon3.StatusActivity;
import com.psiphon3.psiphonlibrary.PsiphonData;
import com.psiphon3.psiphonlibrary.TunnelService;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;


public class Events implements com.psiphon3.psiphonlibrary.Events
{
    public void signalHandshakeSuccess(Context context)
    {
        // Only send this intent if the StatusActivity is
        // in the foreground. If it isn't and we sent the
        // intent, the activity will interrupt the user in
        // some other app.        
        // It's too late to do this check in StatusActivity
        // onNewIntent.
        
        if (PsiphonData.getPsiphonData().getStatusActivityForeground())
        {
            Intent intent = new Intent(
                    StatusActivity.HANDSHAKE_SUCCESS,
                    null,
                    context,
                    com.psiphon3.StatusActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public void signalUnexpectedDisconnect(Context context)
    {
        // Only launch the intent if the browser is the current
        // task. We don't want to interrupt other apps; and in
        // the case of our app (currently), only the browser needs
        // to be interrupted.

        // TODO: track with onResume/onPause flag, as per:
        // http://stackoverflow.com/questions/3667022/android-is-application-running-in-background
        // In the meantime, the imprecision of the getRunningTasks method is acceptable in
        // our current case since it's is only used to not annoy the user.
        
        ActivityManager activityManager =
                (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> runningTasks = activityManager.getRunningTasks(1);        
        
        if (runningTasks.size() > 0 &&
                runningTasks.get(0).baseActivity.flattenToString().compareTo(
                        "com.psiphon3/org.zirco.ui.activities.MainActivity") == 0)
        {
            Intent intent = new Intent(
                    StatusActivity.UNEXPECTED_DISCONNECT,
                    null,
                    context,
                    com.psiphon3.StatusActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
    
    public void signalTunnelStarting(Context context)
    {
        // Local broadcast to any existing status screen
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        Intent intent = new Intent(StatusActivity.TUNNEL_STARTING);
        localBroadcastManager.sendBroadcast(intent);
    }
    
    public void signalTunnelStopping(Context context)
    {
        // Local broadcast to any existing status screen
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        Intent intent = new Intent(StatusActivity.TUNNEL_STOPPING);
        localBroadcastManager.sendBroadcast(intent);
    }

    public Intent pendingSignalNotification(Context context)
    {
        Intent intent = new Intent(
                "ACTION_VIEW",
                null,
                context,
                com.psiphon3.StatusActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
    
    static public void displayBrowser(Context context)
    {
        displayBrowser(context, null);
    }

    static public void displayBrowser(Context context, Uri uri)
    {
        if (PsiphonData.getPsiphonData().getTunnelWholeDevice())
        {
            // TODO: support multiple home pages in whole device mode. This is
            // disabled due to the case where users haven't set a default browser
            // and will get the prompt once per home page.
            for (String homePage : PsiphonData.getPsiphonData().getHomePages())
            {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(homePage));
                context.startActivity(browserIntent);
                break; // Only open the first home page
            }
        }
        else
        {
            Intent intent = new Intent(
                    "ACTION_VIEW",
                    uri,
                    context,
                    org.zirco.ui.activities.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    
            // This intent displays the Zirco browser.
            // We use "extras" to communicate Psiphon settings to Zirco, which
            // is packaged as an independent component (so it's can't access,
            // e.g., PsiphonConstants or PsiphonData). Note that the Zirco code
            // is customized. When Zirco is first created, it will use the localProxyPort
            // and homePages extras to set the proxy preference and open tabs for
            // each home page, respectively. When the intent triggers an existing
            // Zirco instance (and it's a singleton) the extras are ignored and the
            // browser is displayed as-is.
            // When a uri is specified, it will open as a new tab. This is
            // independent of the home pages.
            
            intent.putExtra("localProxyPort", PsiphonData.getPsiphonData().getHttpProxyPort());
            intent.putExtra("homePages", PsiphonData.getPsiphonData().getHomePages());
            intent.putExtra("serviceClassName", TunnelService.class.getName());        
            intent.putExtra("statusActivityClassName", StatusActivity.class.getName());
            intent.putExtra("feedbackActivityClassName", FeedbackActivity.class.getName());
            
            context.startActivity(intent);
        }
    }
}
