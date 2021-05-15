package com.brouken.player;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;

import com.github.javiersantos.appupdater.AppUpdaterUtils;
import com.github.javiersantos.appupdater.enums.AppUpdaterError;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.github.javiersantos.appupdater.objects.Update;

import io.github.g00fy2.versioncompare.Version;

public class UpdateCheckJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        final Context context = this;
        final AppUpdaterUtils appUpdaterUtils = new AppUpdaterUtils(this)
                .setUpdateFrom(UpdateFrom.GITHUB)
                .setGitHubUserAndRepo("moneytoo", "Player")
                .withListener(new AppUpdaterUtils.UpdateListener() {
                    @Override
                    public void onSuccess(Update update, Boolean isUpdateAvailable) {
                        final String titleUpdate = getResources().getString(R.string.appupdater_update_available);
                        final String descriptionUpdate = String.format(getResources().getString(R.string.appupdater_update_available_description_notification), update.getLatestVersion(), getString(R.string.app_name));
                        if (new Version(update.getLatestVersion()).isHigherThan(BuildConfig.VERSION_NAME)) {
                            UtilsFlavor.showUpdateAvailableNotification(context, titleUpdate, descriptionUpdate, update.getUrlToDownload());
                        }
                        jobFinished(params, false);
                    }

                    @Override
                    public void onFailed(AppUpdaterError error) {
                        jobFinished(params, true);
                    }
                });
        appUpdaterUtils.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
