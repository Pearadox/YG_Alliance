package com.pearadox.yg_alliance;

import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
// === DEBUG  ===
import android.util.Log;
import android.widget.Toast;

import com.cpjd.main.Settings;
import com.cpjd.main.TBA;
import com.cpjd.models.Event;
import com.cpjd.models.Match;
import com.cpjd.models.Team;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;


public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";        // This CLASS name
    String Pearadox_Version = " ";      // initialize
    Spinner spinner_Device, spinner_Event;
    TextView txt_EvntCod, txt_EvntDat, txt_EvntPlace;
    ArrayAdapter<String> adapter_Event;
    Button btn_Teams, btn_Match_Sched, btn_Spreadsheet;
    Team[] BAteams;
    public static int BAnumTeams = 0;                      // # of teams from Blue Alliance
    public String[] teamsRed;
    public String[] teamsBlue;
    private FirebaseDatabase pfDatabase;
    private DatabaseReference pfMatchData_DBReference;
    matchData match_inst = new matchData();
    String destFile;
    String prevTeam ="";
    int startRow = 3; int lastRow = 0;
    BufferedWriter bW;
    String tmName=""; String tmRank=""; String tmWLT=""; String tmOPR=""; String tmKPa=""; String tmTPts="";
    Event BAe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "******* Starting Yellow-Green Alliance  *******");

        try {
            Pearadox_Version = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        Toast toast = Toast.makeText(getBaseContext(), "Pearadox Scouting App ©2017  Ver." + Pearadox_Version, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.show();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        preReqs(); 				        // Check for pre-requisites

        Spinner spinner_Event = (Spinner) findViewById(R.id.spinner_Event);
        String[] events = getResources().getStringArray(R.array.event_array);
        adapter_Event = new ArrayAdapter<String>(this, R.layout.list_layout, events);
        adapter_Event.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_Event.setAdapter(adapter_Event);
        spinner_Event.setSelection(0, false);
        spinner_Event.setOnItemSelectedListener(new event_OnItemSelectedListener());

        btn_Teams = (Button) findViewById(R.id.btn_Teams);
        btn_Match_Sched = (Button) findViewById(R.id.btn_Match_Sched);
        btn_Spreadsheet = (Button) findViewById(R.id.btn_Spreadsheet);
        btn_Teams.setEnabled(false);
        btn_Match_Sched.setEnabled(false);
        btn_Spreadsheet.setEnabled(false);
        txt_EvntCod = (TextView) findViewById(R.id.txt_EvntCod);
        txt_EvntDat = (TextView) findViewById(R.id.txt_EvntDat);
        txt_EvntPlace = (TextView) findViewById(R.id.txt_EvntPlace);
        txt_EvntCod.setText("");   // Event Code
        txt_EvntDat.setText("");                      // Event Date
        txt_EvntPlace.setText("");                      // Event Location

        TBA.setID("Pearadox", "YG_Alliance", "V1");
        final TBA tba = new TBA();
        Settings.FIND_TEAM_RANKINGS = true;
        Settings.GET_EVENT_TEAMS = true;
        Settings.GET_EVENT_MATCHES = true;
//        Settings.GET_EVENT_ALLIANCES = true;
//        Settings.GET_EVENT_AWARDS = true;

//        Event e = tba.getEvent("txlu", 2017);

/* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ */
        btn_Teams.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "  btn_Teams setOnClickListener  " + Pearadox.FRC_ChampDiv);

                Team[] teams = tba.getTeams(Pearadox.FRC_ChampDiv, 2017);
                Log.d(TAG, " Team array size = " + teams.length);
                if (teams.length > 0) {
                    String destFile = Pearadox.FRC_ChampDiv + "_Teams" + ".json";
                    try {
                        File prt = new File(Environment.getExternalStorageDirectory() + "/download/FRC5414/" + destFile);
                        BufferedWriter bW;
                        bW = new BufferedWriter(new FileWriter(prt, false));    // true = Append to existing file
                        bW.write("[" + "\n");
                        for (int i = 0; i < teams.length; i++) {
                            String tnum = String.format("%1$4s", teams[i].team_number);
                            Log.d(TAG, " Team = " + tnum);
                            bW.write("    {    \"team_num\":\"" + tnum + "\", " + "\n");
                            bW.write("         \"team_name\":\"" + teams[i].nickname + "\", " + "\n");
                            bW.write("         \"team_loc\":\"" + teams[i].location + "\" " + "\n");

                            if (i == teams.length - 1) {       // Last one?
                                bW.write("    } " + "\n");
                            } else {
                                bW.write("    }," + "\n");
                            }
                        } // end For # teams
                        //=====================================================================

                        bW.write("]" + "\n");
                        bW.write(" " + "\n");
                        bW.flush();
                        bW.close();
                        Toast toast = Toast.makeText(getBaseContext(), "*** '" + Pearadox.FRC_Event + "' Teams file (" + teams.length + " teams) written to SD card ***", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                        toast.show();
                    } catch (FileNotFoundException ex) {
                        System.out.println(ex.getMessage() + " not found in the specified directory.");
                        System.exit(0);
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }else {
                    final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                    Toast toast = Toast.makeText(getBaseContext(), "** There are _NO_ teams for '" + Pearadox.FRC_ChampDiv + "' **", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                }
            }
        });

/* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ */
        btn_Match_Sched.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "  btn_Match_Sched setOnClickListener  ");
                Event event = new TBA().getEvent("2017" + Pearadox.FRC_ChampDiv);       // GLF 4/12
                Match[] matches = event.matches;
                Log.d(TAG, " Matches size = " + matches.length);

                //----------------------------------------
                if (matches.length > 0) {
                    System.out.println("Match name: " + matches[0].comp_level + " Time: " + matches[0].time_string + " Time (long in ms): " + matches[0].time);
                    Date date1 = new Date(matches[0].time);
                    DateFormat formatter1 = new SimpleDateFormat("HH:mm:ss:SSS");
                    String dateFormatted = formatter1.format(date1);
                    Log.e(TAG, " Time = "  + dateFormatted);
                    System.out.println("Match name: "+matches[3].comp_level + " Time: "+matches[3].time_string + " Time (long in ms): " +matches[3].time);
                    Date date2 = new Date(matches[3].time);
                    DateFormat formatter2 = new SimpleDateFormat("HH:mm");
                    String dateFormatted2 = formatter2.format(date2);
                    Log.e(TAG, " Time = "  + dateFormatted2);
                }  else {
                    final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                    Toast toast = Toast.makeText(getBaseContext(), "***  Data from the Blue Alliance is _NOT_ available this session  ***", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                }
                //----------------------------------------
                int qm;
                String mn, r1, r2, r3, b1, b2, b3;
                String matchFile = Pearadox.FRC_ChampDiv + "_Match-Sched" + ".json";
                if (matches.length > 0) {
                    // The comp level variable includes an indentifier for whether it's practice, qualifying, or playoff
                    try {
                        File prt = new File(Environment.getExternalStorageDirectory() + "/download/FRC5414/" + matchFile);
                        BufferedWriter bW;
                        bW = new BufferedWriter(new FileWriter(prt, false));    // true = Append to existing file
                        bW.write("[" + "\n");
                        for (int i = 0; i < matches.length; i++) {
                            Log.d(TAG, " Comp = " + matches[i].comp_level);
                            if (matches[i].comp_level.matches("qm")) {
                                bW.write(" {\"time\":\"" + matches[i].time_string + "\", ");
                                mn = String.valueOf(matches[i].match_number);
                                if (mn.length() < 2) {mn = "0" + mn;}   // make it at least 2-digits
                                Log.d(TAG, " match# = " + mn);
                                bW.write("  \"mtype\":\"Qualifying\",  \"match\": \"Q" + mn + "\", ");
                                teamsRed = matches[i].redTeams.clone();
                                r1 = teamsRed[0].substring(3, teamsRed[0].length());
                                if (r1.length() < 4) {r1 = " " + r1;}
                                Log.d(TAG, " R1 = " + r1);
                                r2 = teamsRed[1].substring(3, teamsRed[1].length());
                                if (r2.length() < 4) {r2 = " " + r2;}
                                r3 = teamsRed[2].substring(3, teamsRed[2].length());
                                if (r3.length() < 4) {r3 = " " + r3;}
                                bW.write(" \"r1\":\"" + r1 + "\",  \"r2\": \"" + r2 + "\", \"r3\":\"" + r3 + "\",");
                                teamsBlue = matches[i].blueTeams.clone();
                                b1 = teamsBlue[0].substring(3, teamsBlue[0].length());
                                if (b1.length() < 4) {b1 = " " + b1;}
                                b2 = teamsBlue[1].substring(3, teamsBlue[1].length());
                                if (b2.length() < 4) {b2 = " " + b2;}
                                b3 = teamsBlue[2].substring(3, teamsBlue[2].length());
                                if (b3.length() < 4) {b3 = " " + b3;}
                                bW.write(" \"b1\":\"" + b1 + "\",  \"b2\": \"" + b2 + "\", \"b3\":\"" + b3 + "\"");

                                if (i == matches.length -1) {       // Last one?
                                    bW.write("} " + "\n");
                                }  else {
                                    bW.write("}," + "\n");
                                }
                            }  else {
                                Log.d(TAG, "******* NOT 'qm' ********* " );
                                System.out.println(matches[i].set_number);
                                System.out.println(matches[i].event_key);
                                System.out.println(matches[i].time_string);
                                System.out.println(matches[i].key);
                            }
                        }  // end For # matches
                        //=====================================================================

                        bW.write("]" + "\n");
                        bW.write(" " + "\n");
                        bW.flush();
                        bW.close();
                        Toast toast = Toast.makeText(getBaseContext(), "*** '" + Pearadox.FRC_Event + "' Matches file written to SD card ***" , Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                        toast.show();
                    } catch (FileNotFoundException ex) {
                        System.out.println(ex.getMessage() + " not found in the specified directory.");
                        System.exit(0);
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }  // end Try/Catch
                }  else {
                    Toast toast = Toast.makeText(getBaseContext(), "☆☆☆ No Match data exists for this event yet (too early!)  ☆☆☆" , Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                }
            }
        });


/* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ */
    btn_Spreadsheet.setOnClickListener(new View.OnClickListener() {
    public void onClick(View v) {
        Log.i(TAG, "  btn_Spreadsheet setOnClickListener  ");
        Log.e(TAG, "***** Matches # = "  + Pearadox.Matches_Data.size());   // Done in Event Click Listner
//        Toast toast1 = Toast.makeText(getBaseContext(), "FRC5414 ©2017  *** Match Data loaded = " + Pearadox.Matches_Data.size() + " ***" , Toast.LENGTH_LONG);
//        toast1.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
//        toast1.show();
        String new_comm="";

        destFile = Pearadox.FRC_Event + "_MatchData" + ".csv";
        try {
            File prt = new File(Environment.getExternalStorageDirectory() + "/download/FRC5414/" + destFile);
            bW = new BufferedWriter(new FileWriter(prt, false));    // true = Append to existing file
            bW.write(Pearadox.FRC_Event.toUpperCase() + " - " + Pearadox.FRC_EventName +"  \n");
            // Write Excel/Spreadsheet Header for each column
            bW.write("Team,Match,Auto Mode,Rope?,Carry Fuel?,Fuel Amt,Carry Gear?,Start Pos,");
            bW.write("Cross Baseline?,Gears Placed,Gears Attempted,Gear Post,Shoot HG?,HG %,Shoot LG?,LG %,");
            bW.write("Act Hopper?,Fuel Collected,Stop Position,Auto Comment,|,");

            bW.write("Pickup Gears?,Gears Placed,Gears Attempted,# Cycles,Shoot HG?,HG %,Shoot LG?,LG %,");
            bW.write("Climb Attempt?,Touchpad Activated?,Climb Success?,Tele Comment,|,");

            bW.write("Lost Parts?,Lost Comms?,Good Def?,Lane?,Blocking?,Hopper Dump?,Gear Block?,");
            bW.write("Num Penalties,Date-Time Saved,Final Comment,||,Last, First");
            bW.write(",|,Team,Rank,W-L-T,OPR,kPa,Touch Pts,|");
            bW.write(",Weighted ALL,Weighted Last-3,Auto Gear ALL,Auto Gear Last-3,Tele Gear ALL,Tele Gear Last-3,Climbs ALL,Climbs Last-3,Auto Gear Center,Auto Gear Left-Side,Auto Gear Right-Side, TOTAL Auto Sides");

            bW.write(" " + "\n");
            prevTeam ="";
            //=====================================================================
            for (int i = 0; i < Pearadox.Matches_Data.size(); i++) {
                match_inst = Pearadox.Matches_Data.get(i);      // Get instance of Match Data
                if (!match_inst.getTeam_num().matches(prevTeam)) {      // Same team?
                    if (i > 0) {
//                        Log.w(TAG, "Prev: " + prevTeam + "  New: " + match_inst.getTeam_num() + "  Start: " + startRow + "  i=" + i);
                        wrtHdr();
                    }  else {
                        prevTeam = match_inst.getTeam_num();
                    }
                    lastRow = startRow - 1;
                }
                bW.write(match_inst.getTeam_num() + "," + match_inst.getMatch() + ",");
                //----- Auto -----
                bW.write(match_inst.isAuto_mode() + "," + match_inst.isAuto_rope() + "," + match_inst.isAuto_carry_fuel() + "," + match_inst.getAuto_fuel_amount() + "," + match_inst.isAuto_gear() + "," + match_inst.getAuto_start() + ",");
                bW.write(match_inst.isAuto_baseline() + "," + match_inst.getAuto_gears_placed() + "," + match_inst.getAuto_gears_attempt() + "," + match_inst.getAuto_gear_pos() + "," + match_inst.isAuto_hg() + "," + match_inst.getAuto_hg_percent() + "," + match_inst.isAuto_lg() + "," + match_inst.getAuto_lg_percent() + ",");
                new_comm = StringEscapeUtils.escapeCsv(match_inst.getAuto_comment());
                bW.write(match_inst.isAuto_act_hopper() + "," + match_inst.getAuto_fuel_collected() + "," + match_inst.getAuto_stop() + "," + new_comm + "," + "|" + ",");
                //----- Tele -----
                bW.write(match_inst.isTele_gear_pickup() + "," + match_inst.getTele_gears_placed() + "," + match_inst.getTele_gears_attempt() + "," + match_inst.getTele_cycles() + "," + match_inst.isTele_hg() + "," + match_inst.getTele_hg_percent() + "," + match_inst.isTele_lg() + "," + match_inst.getTele_lg_percent() + ",");
                String y = match_inst.getTele_comment();
                new_comm = StringEscapeUtils.escapeCsv(match_inst.getTele_comment());
                bW.write(match_inst.isTele_climb_attempt() + "," + match_inst.isTele_touch_act() + "," + match_inst.isTele_climb_success() + "," + new_comm + "," + "|" + ",");
                //----- Final -----
                bW.write(match_inst.isFinal_lostParts() + "," + match_inst.isFinal_lostComms() + "," + match_inst.isFinal_defense_good() + "," + match_inst.isFinal_def_Lane() + "," + match_inst.isFinal_def_Block() + "," + match_inst.isFinal_def_Hopper() + "," + match_inst.isFinal_def_Gear() + ",");
                String x = match_inst.getFinal_comment();
                new_comm = StringEscapeUtils.escapeCsv(match_inst.getFinal_comment());
                bW.write(match_inst.getFinal_num_Penalties() + "," + match_inst.getFinal_dateTime() + "," + new_comm + "," + "||" + "," + match_inst.getFinal_studID() + ",|,,,,,,,|");
                //-----------------
                bW.write(" " + "\n");
                lastRow = lastRow + 1;
//                Log.w(TAG, match_inst.getTeam_num() + "  Last: " + lastRow);
                if (i == Pearadox.Matches_Data.size() -1) {       // Last one?
                    wrtHdr();
                }
            } // End For

            //=====================================================================
            bW.write(" " + "\n");
            bW.flush();
            bW.close();
            Toast toast = Toast.makeText(getBaseContext(), "*** '" + Pearadox.FRC_Event.toUpperCase() + "' Match Data Spreadsheet written to SD card ***" , Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
            btn_Spreadsheet.setEnabled(false);      // turn off button (done)
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage() + " not found in the specified directory.");
            System.exit(0);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }  // end Try/Catch

        }
    });
}

    private void wrtHdr() {
//        Log.i(TAG, " wrtHdr  " + prevTeam);
        try {
            bW.write(prevTeam + ",'***,");
            bW.write(",,,,,,'TOTAL >,=SUM($J" + startRow + ":$J" + lastRow + "),=SUM($K" + startRow + ":$K" + lastRow + ") ");
            String escJK = StringEscapeUtils.escapeCsv("=IF($K" + (lastRow+1) +">0,$J" + (lastRow+1) + "/$K" + (lastRow+1) + ",0)");
            bW.write(",'RATIO >," +  escJK);
            String escL3 = StringEscapeUtils.escapeCsv("=AVERAGE(OFFSET(INDIRECT(\"J\"&ROW()),-3,0,3,1))");
            bW.write(",'ALL>,=($J" + (lastRow+1) + "/" + ((lastRow-startRow)+1) + "),'LAST 3>," + escL3);
            bW.write(",,,,|,'TOTAL >,=SUM($W" + startRow + ":$W" + lastRow + "),=SUM($X" + startRow + ":$X" + lastRow + ")");
            bW.write(",'RATIO >,=$W" + (lastRow+1) + "/" + ((lastRow-startRow)+1) );
            bW.write(",'Last 3>,=Sum($W" + (lastRow-2) + ":$W" + (lastRow) + ")/3" );    // Tele Gears Last 3

            String esc$AD = StringEscapeUtils.escapeCsv("=(COUNTIF($AD" + startRow + ":$AD" + lastRow + ",TRUE))");
            String esc$AF = StringEscapeUtils.escapeCsv("=(COUNTIF($AF" + startRow + ":$AF" + lastRow + ",TRUE))");
            String escAD$AF = StringEscapeUtils.escapeCsv("=IF($AD" + (lastRow+1) +">0,$AE" + (lastRow+1) + "/" + ((lastRow-startRow)+1) + ",0)");
            String esc$AD3 = StringEscapeUtils.escapeCsv("=(COUNTIF($AF" + (lastRow-2) + ":$AF" + (lastRow) + ",TRUE))");
            bW.write(",'TOTAL >,"+ esc$AD + "," + esc$AF + "," + escAD$AF + "," + esc$AD3 + "/3,,,,,,,,,,,,||,,,|,");
            gatherBA(prevTeam);
            bW.write(tmName + "," + tmRank + ",'"+tmWLT + ","+tmOPR + ","+tmKPa + ","+tmTPts + ",|");

            bW.write(",=(($AF" + (lastRow+1) +"*2) + $Z" + (lastRow+1) + " + $O" + (lastRow+1) +") / 3");   // Weighted ALL
            bW.write(",=(($AG" + (lastRow+1) +"*2) + $AB" + (lastRow+1) + " + $Q" + (lastRow+1) +") / 3,");  // Weighted Last 3
            bW.write("=$O$" + (lastRow+1) + ",=$Q$" + (lastRow+1) + ",");          // Auto Gears (ALL & Last 3)
            bW.write("=$Z$" + (lastRow+1) + ",=$AB$" + (lastRow+1) + ",");         // Tele Gears (ALL & Last 3)
            bW.write("=$AF$" + (lastRow+1) + ",=$AG$" + (lastRow+1) + ",");        // Climbs (ALL & Last 3)
            String escL = StringEscapeUtils.escapeCsv("=COUNTIF($L$" + startRow  + ":$L$" + (lastRow) + ",\"2\")");
            bW.write(escL + ",");           // Auto Center Gears
            String escS1 = StringEscapeUtils.escapeCsv("=COUNTIF($L$" + startRow  + ":$L$" + (lastRow) + ",\"1\")");
            bW.write(escS1 + ",");           // Auto Left-Side Gears
            String escS3 = StringEscapeUtils.escapeCsv("=COUNTIF($L$" + startRow  + ":$L$" + (lastRow) + ",\"3\")");
            bW.write(escS3 + ",=$BM$" + (lastRow+1) + "+ $BN$" + (lastRow+1) + ",");      // TOTAL Auto Side Gears
            //=============================
            bW.write(" " + "\n");   // End-of-Line
            prevTeam = match_inst.getTeam_num();
            startRow = (lastRow) + 2;              // Start row for new team
//            Log.w(TAG,"  Last: " + lastRow);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void gatherBA(String teamNo) {
//        Log.i(TAG, " gatherBA  " + teamNo);
        for (int i = 0; i < BAnumTeams; i++) {
            if (BAe.teams[i].team_number == Long.parseLong(teamNo.trim())) {
                tmName = BAe.teams[i].nickname;
                tmRank = String.valueOf(BAe.teams[i].rank);
                tmWLT = BAe.teams[i].record;
                tmOPR = String.format("%3.3f",((new TBA().fillOPR(BAe, BAe.teams[i]).opr)));
//                Log.w(TAG,"  OPR: " + BAe.teams[i].opr);
//                tmOPR = String.format("%3.3f",(BAe.teams[i].opr));
                tmKPa = String.valueOf(BAe.teams[i].pressure);
                tmTPts = String.valueOf(BAe.teams[i].touchpad);
//                System.out.println(tmName+" "+tmRank+" "+ tmWLT+" "+tmOPR+" "+tmKPa+" "+tmTPts + " \n");
                break;      // exit For - found team
            }
        }
    }

    private String removeLine(String comment) {
        String x = "";
//        ToDo - Carriage return
        if (comment.contains(",")) {
//            Log.w(TAG, " %$^&#!! COMMA  " + match_inst.getMatch() + "," + match_inst.getTeam_num() + "[" + comment + "]");
//            int Comma = comment.indexOf(",");
            x = comment.replaceAll(",", ";");
//            x = comment.substring(0, Comma) +"•"+  comment.substring(Comma + 1);
//            Log.w(TAG, "X: '" + x + "'");
        }
        return x;
    }


    // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    private void preReqs() {
        boolean isSdPresent;
        isSdPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        Log.w(TAG, "SD card: " + isSdPresent);
        if (isSdPresent) {        // Make sure FRC directory is there
            File extStore = Environment.getExternalStorageDirectory();
            File directFRC = new File(Environment.getExternalStorageDirectory() + "/download/FRC5414");
            if (!directFRC.exists()) {
                if (directFRC.mkdir()) {
                }        //directory is created;
            }
            Log.i(TAG, "FRC files created");
//        Toast toast = Toast.makeText(getBaseContext(), "FRC5414 ©2017  *** Files initialied ***" , Toast.LENGTH_LONG);
//        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
//        toast.show();
        }  else {
            Toast.makeText(getBaseContext(), "There is no SD card available", Toast.LENGTH_LONG).show();
        }
    }

/* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ */
private class event_OnItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
    public void onItemSelected(AdapterView<?> parent,
                               View view, int pos, long id) {
        String ev = parent.getItemAtPosition(pos).toString();
        Pearadox.FRC_EventName = ev;
        Log.d(TAG, ">>>>> Event '" + Pearadox.FRC_EventName + "'");
        switch (ev) {
            case "The Remix 2017 (Woodlands)":     // txsc
                Pearadox.FRC_Event = "txrm";
                Pearadox.FRC_ChampDiv = "txrm";
                break;
            case "UIL State Championship (Austin)":     // txsc
                Pearadox.FRC_Event = "txsc";
                Pearadox.FRC_ChampDiv = "txsc";
                break;
            case "FIRST Championship (Houston)":        // cmptx
                Pearadox.FRC_Event = "cmptx";
                Pearadox.FRC_ChampDiv = "gal";          // Galileo Division
                break;
            case "Brazos Valley Regional":              // txwa
                Pearadox.FRC_Event = "txwa";
                Pearadox.FRC_ChampDiv = "txwa";
                break;
            case ("Lone Star Central Regional"):        // txho
                Pearadox.FRC_Event = "txho";
                Pearadox.FRC_ChampDiv = "txho";
                break;
            case ("Hub City Regional"):             // txlu
                Pearadox.FRC_Event = "txlu";
                Pearadox.FRC_ChampDiv = "txlu";
                break;
            default:                // ?????
                Toast.makeText(getBaseContext(), "Event code not recognized", Toast.LENGTH_LONG).show();
                Pearadox.FRC_Event = "zzzz";
        }
        Log.d(TAG, " Event code = '" + Pearadox.FRC_Event + "'");
        Log.d(TAG, "*** Event ***");
        Event e = new TBA().getEvent("2017" + Pearadox.FRC_Event);
        // Print general event info
        System.out.println(e.name);
        System.out.println(e.location);
        System.out.println(e.start_date);
        System.out.println("\n");
        txt_EvntCod = (TextView) findViewById(R.id.txt_EvntCod);
        txt_EvntDat = (TextView) findViewById(R.id.txt_EvntDat);
        txt_EvntPlace = (TextView) findViewById(R.id.txt_EvntPlace);
        txt_EvntCod.setText(Pearadox.FRC_Event.toUpperCase());  // Event Code
        txt_EvntDat.setText(e.start_date);                      // Event Date
        txt_EvntPlace.setText(e.location);                      // Event Location

        btn_Teams.setEnabled(true);
        btn_Match_Sched.setEnabled(true);

        pfDatabase = FirebaseDatabase.getInstance();
        pfMatchData_DBReference = pfDatabase.getReference("match-data/" + Pearadox.FRC_Event);    // Match Data
        addMD_VE_Listener(pfMatchData_DBReference.orderByChild("team_num"));        // Load _ALL_ Matches in team order GLF 4/18

    }
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing.
    }
}


    // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    private void addMD_VE_Listener(final Query pfMatchData_DBReference) {
        pfMatchData_DBReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i(TAG, "<<<< addMD_VE_Listener >>>>     Match Data ");
                Pearadox.Matches_Data.clear();
                matchData mdobj = new matchData();
                Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();   /*get the data children*/
                Iterator<DataSnapshot> iterator = snapshotIterator.iterator();
                while (iterator.hasNext()) {
                    mdobj = iterator.next().getValue(matchData.class);
                    Pearadox.Matches_Data.add(mdobj);
                }
                Log.w(TAG, "***** Matches Loaded from Firebase. # = "  + Pearadox.Matches_Data.size());
                Toast toast1 = Toast.makeText(getBaseContext(), "FRC5414 ©2017  *** Match Data loaded = " + Pearadox.Matches_Data.size() + " ***" , Toast.LENGTH_LONG);
                toast1.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast1.show();
// ----------  Blue Alliance  -----------
                Settings.GET_EVENT_STATS = false;
                TBA t = new TBA();
                BAe = new TBA().getEvent("2017" + Pearadox.FRC_ChampDiv);
                BAteams = BAe.teams.clone();
                BAnumTeams = BAteams.length;

                btn_Spreadsheet.setEnabled(true);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                /*listener failed or was removed for security reasons*/
            }
        });
    }


//###################################################################
//###################################################################
//###################################################################
@Override
public void onStart() {
    super.onStart();
    Log.v(TAG, ">>>>  yg_alliance onStart  <<<<");

}
    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
     }
    @Override
    public void onStop() {
        super.onStop();
        Log.v(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "OnDestroy ");
    }

}
