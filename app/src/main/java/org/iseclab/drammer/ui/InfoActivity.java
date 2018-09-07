package org.iseclab.drammer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.iseclab.drammer.R;

public class InfoActivity extends AppCompatActivity {

    private ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        mActionBar = this.getSupportActionBar();
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setCustomView(R.layout.action_bar_title);
        TextView title = ((TextView) findViewById(R.id.action_bar_title));
        title.setText(getResources().getString(R.string.app_name_ascii));
        //this.getSupportActionBar().hide();

        // make links in textview clickable
        TextView text = (TextView) findViewById(R.id.text_info);
        text.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar_info, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menuback) {
            startActivity(new Intent(InfoActivity.this, MainActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
