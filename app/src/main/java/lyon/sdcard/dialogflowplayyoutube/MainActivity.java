package lyon.sdcard.dialogflowplayyoutube;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import lyon.sdcard.dialogflowplayyoutube.DialogFlow.DialogFlowInit;
import lyon.sdcard.dialogflowplayyoutube.Youtube.Play.YoutubeFragment;
import lyon.sdcard.dialogflowplayyoutube.Youtube.Search.SearchYoutube;
import lyon.sdcard.dialogflowplayyoutube.Youtube.YoutubePoster;

public class MainActivity extends AppCompatActivity {

    String TAG = MainActivity.class.getSimpleName();
    DialogFlowInit dialogFlowInit;
    ArrayList<Item> arrayList = new ArrayList<>();
    MainListViewAdapter adapter;
    final int TOAST = 1;
    public final int NOTIFYCHANGE=2;
    Button sayBtn;
    ListView mainListView;
    List<YoutubePoster> youtubePosters;
    String nexttoken;
    LinearLayoutManager mLayoutManager;
    GridLayoutManager gridLayoutManager;
    YoutubeAdapter mAdapter;
    RecyclerView mRecyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arrayList = new ArrayList<>();
        mainListView = (ListView)findViewById(R.id.mainListView);
        adapter = new MainListViewAdapter(this,arrayList);
        mainListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        dialogFlowInit = new DialogFlowInit(this){
            @Override
            public void DialogFlowSpeech(String speech) {
                super.DialogFlowSpeech(speech);

            }

            @Override
            public void DialogFlowAction(JSONObject jsonObject) {
                super.DialogFlowAction(jsonObject);
                String action = jsonObject.optString("action");
                if(action.equals("play_music")){
                    String artist = jsonObject.optString("artist");
                    String song = jsonObject.optString("song");
                   Toast.makeText(MainActivity.this,"播放:"+artist+" 歌曲:"+song,Toast.LENGTH_LONG).show();
                    arrayList.add(toItem(Item.SPEAKTOGOTYPE ,jsonObject.toString()));
                    adapter.notifyDataSetChanged();
                    searchYoutube(artist+" "+song);

                }
            }
        };

        sayBtn = (Button) findViewById(R.id.sayBtn);
        sayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pleaseToSay();
            }
        });
        youtubePosters = new ArrayList<>();
        mAdapter = new YoutubeAdapter(youtubePosters);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclesView);
        mRecyclerView.setAdapter(mAdapter);
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        gridLayoutManager = new GridLayoutManager(this,6);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 200){
            if(resultCode == RESULT_OK && data != null){
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String req = result.get(0);
                arrayList.add(toItem(Item.CUSTOMERTYPE ,req));
                adapter.notifyDataSetChanged();
                if(dialogFlowInit!=null){
                    dialogFlowInit.setAiRequest(req);
                }

            }
        }
    }

    private Item toItem(int Type , String sss){
        Item itme = new Item();
        itme.Type=Type;
        itme.sss=sss;
        return itme;
    }

    private void pleaseToSay(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "???");
        try{
            startActivityForResult(intent,200);
        }catch (ActivityNotFoundException a){
            Message message = new Message();
            message.what=TOAST;
            message.obj = a.toString();
            mMainHandler.sendMessage(message);
            Log.e(TAG,"pleaseToSay ActivityNotFoundException:"+a);
        }
    }

    private Handler mMainHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case TOAST:
                    String sss = msg.obj.toString();
                    Toast.makeText(getApplicationContext(),"Intent problem:"+sss, Toast.LENGTH_SHORT).show();
                    break;
                case NOTIFYCHANGE:
                    mAdapter.setNotifyDataSetChanged(youtubePosters);
                    playYoutube(0);
                    break;
            }
        }
    };


    private void searchYoutube(String keyWord){
        new SearchYoutube(keyWord){
            public void YoutubePosters(List<YoutubePoster> posters){
                Log.d(TAG,"YoutubeAdapter searchBtn onClick: YoutubePosters size:"+posters.size());
                youtubePosters = posters;

                Message msg = mMainHandler.obtainMessage();
                msg.what=NOTIFYCHANGE;
                msg.obj=youtubePosters;
                mMainHandler.sendMessage(msg);
            }

            @Override
            public void getNextPageToken(String NextPageToken) {
                nexttoken=NextPageToken;
            }

            @Override
            public void getPrevPageToken(String PextPageToken) {

            }
        }.execute();
    }

    private void playYoutube(int position){
        YoutubePoster youtubePoster = youtubePosters.get(position);
        String videoId=youtubePoster.getYoutubeId();
        for(int i=0;i<youtubePosters.size();i++){
            videoId=videoId+","+youtubePosters.get(i).getYoutubeId();
        }

        RelativeLayout youtubePlayerFragment = (RelativeLayout) findViewById(R.id.youtubePlayerFragment);
        youtubePlayerFragment.setVisibility(View.VISIBLE);
        Bundle bundle = new Bundle();
        bundle.putString("videoId",videoId);
        Log.d(TAG,"videoId:"+videoId);
        final YoutubeFragment fragment = new YoutubeFragment();
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.youtubePlayerFragment, fragment)
                .commit();

        fragment.setPlayPauseBtnStatsListener(new YoutubeFragment.setPlayPauseShowListener() {
            @Override
            public boolean isPlayPause(boolean playing) {
//                if(playing){
//                    PlayPauseBtn.setText("playing");
//                }else{
//                    PlayPauseBtn.setText("pause");
//                }

                return false;
            }
        });

    }
}
