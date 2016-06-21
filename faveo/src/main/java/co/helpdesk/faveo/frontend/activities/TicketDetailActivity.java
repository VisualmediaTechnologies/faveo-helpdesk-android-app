package co.helpdesk.faveo.frontend.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import co.helpdesk.faveo.Constants;
import co.helpdesk.faveo.Helper;
import co.helpdesk.faveo.R;
import co.helpdesk.faveo.backend.api.v1.Helpdesk;
import co.helpdesk.faveo.frontend.fragments.ticketDetail.Conversation;
import co.helpdesk.faveo.frontend.fragments.ticketDetail.Detail;
import co.helpdesk.faveo.frontend.receivers.NetworkUtil;
import co.helpdesk.faveo.model.TicketThread;
import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;


public class TicketDetailActivity extends AppCompatActivity implements
        Conversation.OnFragmentInteractionListener,
        Detail.OnFragmentInteractionListener {

    Snackbar networksnackbar;
    ViewPager viewPager;
    ViewPagerAdapter adapter;
    Conversation fragmentConversation;
    Detail fragmentDetail;
    Boolean fabExpanded = false;
    FloatingActionButton fabAdd;
    int cx, cy;
    View overlay;
    EditText editTextInternalNote, editTextCC, editTextReplyMessage;
    Button buttonCreate, buttonSend;
    ProgressDialog progressDialog;

    public static String ticketID;
    public static String ticketNumber;
    public static String ticketOpenedBy;
    public static String ticketSubject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_detail);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        ticketID = getIntent().getStringExtra("TICKET_ID");
        ticketNumber = getIntent().getStringExtra("TICKET_NUMBER");
        ticketOpenedBy = getIntent().getStringExtra("TICKET_OPENED_BY");
        ticketSubject = getIntent().getStringExtra("TICKET_SUBJECT");
        TextView mTitle = (TextView) mToolbar.findViewById(R.id.title);
        mTitle.setText(ticketNumber == null ? "Unknown" : ticketNumber);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        setupViewPager();
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setOnTabSelectedListener(onTabSelectedListener);

        progressDialog = new ProgressDialog(this);
        editTextInternalNote = (EditText) findViewById(R.id.editText_internal_note);
        editTextCC = (EditText) findViewById(R.id.editText_cc);
        editTextReplyMessage = (EditText) findViewById(R.id.editText_reply_message);
        buttonCreate = (Button) findViewById(R.id.button_create);
        buttonSend = (Button) findViewById(R.id.button_send);

        networksnackbar = Snackbar.make(findViewById(android.R.id.content), "No internet connection!", Snackbar.LENGTH_LONG);
        networksnackbar.setAction("SETTINGS", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        networksnackbar.setActionTextColor(getResources().getColor(R.color.blue_300));


        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String note = editTextInternalNote.getText().toString();
                if (note.trim().length() == 0) {
                    Toast.makeText(TicketDetailActivity.this, "Empty message", Toast.LENGTH_LONG).show();
                    return;
                }
                SharedPreferences prefs = getSharedPreferences(Constants.PREFERENCE, 0);
                String userID = prefs.getString("ID", "");
                if (userID != null && userID.length() != 0) {
                    try {
                        note = URLEncoder.encode(note, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    if (NetworkUtil.getConnectivityStatus(v.getContext()) != 0) {
                        new CreateInternalNote(Integer.parseInt(ticketID), Integer.parseInt(userID), note).execute();
                        progressDialog.setMessage("Creating note");
                        progressDialog.show();
                    } else networksnackbar.show();

                } else
                    Toast.makeText(TicketDetailActivity.this, "Wrong userID", Toast.LENGTH_LONG).show();
            }
        });

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cc = editTextCC.getText().toString();
                String replyMessage = editTextReplyMessage.getText().toString();
                if (replyMessage.trim().length() == 0) {
                    Toast.makeText(TicketDetailActivity.this, "Empty message", Toast.LENGTH_LONG).show();
                    return;
                }

                cc = cc.replace(", ", ",");
                if (cc.length() > 0) {
                    String[] multipleEmails = cc.split(",");
                    for (String email : multipleEmails) {
                        if (email.length() > 0 && !Helper.isValidEmail(email)) {
                            Toast.makeText(TicketDetailActivity.this, "Invalid CC", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }

                SharedPreferences prefs = getSharedPreferences(Constants.PREFERENCE, 0);
                String userID = prefs.getString("ID", "");
                if (userID != null && userID.length() != 0) {
                    try {
                        replyMessage = URLEncoder.encode(replyMessage, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    if (NetworkUtil.getConnectivityStatus(v.getContext()) != 0) {
                        new ReplyTicket(Integer.parseInt(ticketID), cc, replyMessage).execute();
                        progressDialog.setMessage("Sending message");
                        progressDialog.show();
                    } else networksnackbar.show();
                } else
                    Toast.makeText(TicketDetailActivity.this, "Wrong userID", Toast.LENGTH_LONG).show();
            }
        });

        fabAdd = (FloatingActionButton) findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCreateRequest();
            }
        });

        overlay = findViewById(R.id.overlay);
        overlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitReveal();
            }
        });

    }

    private void getCreateRequest() {
        final CharSequence[] items = {"Reply", "Internal notes"};
        AlertDialog.Builder builder = new AlertDialog.Builder(TicketDetailActivity.this);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
                if (items[item].equals("Reply")) {
                    cx = (int) fabAdd.getX() + dpToPx(40);
                    cy = (int) fabAdd.getY();
                    fabExpanded = true;
                    fabAdd.hide();
                    enterReveal("Reply");
                } else {
                    cx = (int) fabAdd.getX() + dpToPx(40);
                    cy = (int) fabAdd.getY();
                    fabExpanded = true;
                    fabAdd.hide();
                    enterReveal("Internal notes");
                }
            }
        });
        builder.show();
    }

    public class CreateInternalNote extends AsyncTask<String, Void, String> {
        int ticketID;
        int userID;
        String note;

        public CreateInternalNote(int ticketID, int userID, String note) {
            this.ticketID = ticketID;
            this.userID = userID;
            this.note = note;
        }

        protected String doInBackground(String... urls) {
            return new Helpdesk().postCreateInternalNote(ticketID, userID, note);
        }

        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            if (result == null) {
                Toast.makeText(TicketDetailActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(TicketDetailActivity.this, "Internal notes posted", Toast.LENGTH_LONG).show();
            exitReveal();
        }
    }

    public class ReplyTicket extends AsyncTask<String, Void, String> {
        int ticketID;
        String cc;
        String replyContent;

        public ReplyTicket(int ticketID, String cc, String replyContent) {
            this.ticketID = ticketID;
            this.cc = cc;
            this.replyContent = replyContent;
        }

        protected String doInBackground(String... urls) {
            return new Helpdesk().postReplyTicket(ticketID, cc, replyContent);
        }

        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            if (result == null) {
                Toast.makeText(TicketDetailActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(TicketDetailActivity.this, "Reply posted", Toast.LENGTH_LONG).show();
            try {
                TicketThread ticketThread;
                JSONObject jsonObject = new JSONObject(result);
                JSONObject res = jsonObject.getJSONObject("result");
                String clientPicture = "";
                try {
                    clientPicture = res.getString("profile_pic");
                } catch (Exception e) {

                }
                String messageTitle = "";
                try {
                    messageTitle = res.getString("title");
                } catch (Exception e) {

                }
                String clientName = res.getString("first_name");
                if (clientName.equals("null") || clientName.equals(""))
                    clientName = res.getString("user_name");
                String messageTime = res.getString("created_at");
                String message = res.getString("body");
                String isReply = "true";
                try {
                    isReply = res.getString("is_reply");
                } catch (Exception e) {
                }
                ticketThread = new TicketThread(clientPicture, clientName, messageTime, messageTitle, message, isReply);
                if (fragmentConversation != null) {
                    exitReveal();
                    fragmentConversation.addThreadAndUpdate(ticketThread);
                }
            } catch (JSONException e) {
                if (fragmentConversation != null) {
                    exitReveal();
                }
                e.printStackTrace();
                Toast.makeText(TicketDetailActivity.this, "Unexpected Error ", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupViewPager() {
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        fragmentConversation = new Conversation();
        fragmentDetail = new Detail();
        adapter.addFragment(fragmentConversation, "CONVERSATION");
        adapter.addFragment(fragmentDetail, "DETAIL");
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(onPageChangeListener);
    }

    TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            viewPager.setCurrentItem(tab.getPosition(), true);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    void enterReveal(String type) {
        final View myView = findViewById(R.id.reveal);
        int finalRadius = Math.max(myView.getWidth(), myView.getHeight());
        SupportAnimator anim =
                ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0, finalRadius);
        if (type.equals("Reply")) {
            myView.setVisibility(View.VISIBLE);
            myView.findViewById(R.id.section_reply).setVisibility(View.VISIBLE);
            myView.findViewById(R.id.section_internal_note).setVisibility(View.GONE);
            overlay.setVisibility(View.VISIBLE);
        } else {
            myView.setVisibility(View.VISIBLE);
            myView.findViewById(R.id.section_reply).setVisibility(View.GONE);
            myView.findViewById(R.id.section_internal_note).setVisibility(View.VISIBLE);
            overlay.setVisibility(View.VISIBLE);
        }

        anim.start();
    }

    void exitReveal() {
        final View myView = findViewById(R.id.reveal);
        int finalRadius = Math.max(myView.getWidth(), myView.getHeight());
        SupportAnimator animator = ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0, finalRadius);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(300);
        animator = animator.reverse();
        animator.addListener(new SupportAnimator.AnimatorListener() {

            @Override
            public void onAnimationStart() {

            }

            @Override
            public void onAnimationEnd() {
                fabAdd.show();
                fabExpanded = false;
                myView.setVisibility(View.GONE);
                overlay.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel() {

            }

            @Override
            public void onAnimationRepeat() {

            }

        });
        animator.start();

    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @Override
    public void onBackPressed() {
        if (fabExpanded)
            exitReveal();
        else super.onBackPressed();
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (NetworkUtil.getConnectivityStatus(this) == 0) {
            if (networksnackbar != null)
                networksnackbar.show();
        }

    }
}
