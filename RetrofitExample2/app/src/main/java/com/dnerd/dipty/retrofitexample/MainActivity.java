package com.dnerd.dipty.retrofitexample;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.dnerd.dipty.retrofitexample.adapter.GithubRepoAdapter;
import com.dnerd.dipty.retrofitexample.model.GithubRepo;
import com.dnerd.dipty.retrofitexample.network.RetrofitInstance;
import com.dnerd.dipty.retrofitexample.realm_configuration.ConfigRealm;
import com.dnerd.dipty.retrofitexample.request_interface.RetrofitClientInterface;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static retrofit2.Response.success;

public class MainActivity extends AppCompatActivity {
    private ListView mListView;
    private Realm mRealm;
    private RealmConfiguration mRealmConfig;
    private String task, repoName;
    private GithubRepoAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = findViewById(R.id.git_list_view);

        mRealm.init(MainActivity.this);
        mRealm = ConfigRealm.getRealmCongfiguration();

        Retrofit retrofit = RetrofitInstance.getRetrofitInstance();
        RetrofitClientInterface client = retrofit.create(RetrofitClientInterface.class);

        Call<List<GithubRepo>> call = client.reposForUser("dipty13");

        call.enqueue(new Callback<List<GithubRepo>>() {
            @Override
            public void onResponse(Call<List<GithubRepo>> call, Response<List<GithubRepo>> response) {
                List<GithubRepo> repos = response.body();

                mAdapter = new GithubRepoAdapter(MainActivity.this, repos);
                mListView.setAdapter(mAdapter);

                saveUsingRealm(repos);

            }

            @Override
            public void onFailure(Call<List<GithubRepo>> call, Throwable t) {
                Toast.makeText(MainActivity.this, getString(R.string.error),Toast.LENGTH_LONG).show();

                showSavedDataFromRealm();
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                showUpdateItemDialog(MainActivity.this, i);

            }

        });

    }

    private void showSavedDataFromRealm() {
        List<GithubRepo> repo = mRealm.where(GithubRepo.class).findAll();
        if(repo != null)
        {
            mAdapter = new GithubRepoAdapter(MainActivity.this, repo);
            mListView.setAdapter(mAdapter);
        }
    }

    private void saveUsingRealm( List<GithubRepo> repos) {
        mRealm.beginTransaction();
        mRealm.delete(GithubRepo.class); //removes older value
        mRealm.commitTransaction();

        mRealm.beginTransaction();
        for(int i = 0; i < repos.size(); i++){
            GithubRepo repo = mRealm.createObject(GithubRepo.class);
            repo.setName(repos.get(i).getName());
        }
        mRealm.commitTransaction();
    }

    private void showUpdateItemDialog(Context context, int i) {
        final EditText taskEditText = new EditText(context);
        taskEditText.setText( mAdapter.getItem(i).getName());

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Change Repo Name")
                .setMessage("")
                .setView(taskEditText)
                .setPositiveButton(R.string.change_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        task = String.valueOf(taskEditText.getText());
                        mAdapter.setNotifyOnChange(true);
                        repoName = task;
                        mRealm.beginTransaction();
                        mAdapter.getItem(i).setName(repoName);
                        mRealm.commitTransaction();
                    }
                })
                .setNegativeButton(R.string.cancel_button, null)
                .create();
        dialog.show();

    }
}
