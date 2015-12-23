package com.uberspot.a2048_sdk;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

public class EMailSender {

    // if user didn't define own chooser title
    // use default
    public static final String CHOOSER_TITLE_DEFAULT = "Send email..";

    public static String sChooserTitle = CHOOSER_TITLE_DEFAULT;

    public static void sendEmail(
            Context ctx,
            String[] recipients,
            String subject,
            String body,
            ArrayList<Uri> attachments
    )
    {

        Intent intent = new Intent();

        intent.setType("*/*");

        if (recipients != null && recipients.length > 0)
			intent.putExtra(Intent.EXTRA_EMAIL, recipients);
		
		if (!TextUtils.isEmpty(subject))
			intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		
		if (!TextUtils.isEmpty(body))			
			intent.putExtra(Intent.EXTRA_TEXT, body);
		
		if (attachments != null && attachments.size() > 0) {
			intent.setAction(Intent.ACTION_SEND_MULTIPLE);
			intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments);
		} else
			intent.setAction(Intent.ACTION_SEND);
			
		Intent chooserIntent = Intent.createChooser(intent,
                                                    sChooserTitle
        );
		chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		ctx.startActivity(chooserIntent);
	}
	
	public static void setChooserTitle(String chooserTitle) {
		sChooserTitle = chooserTitle;
	}
}
