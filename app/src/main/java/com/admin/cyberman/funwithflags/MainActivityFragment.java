package com.admin.cyberman.funwithflags;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.animation.Animator;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.app.AlertDialog;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.io.IOException;
import java.io.InputStream;




public class MainActivityFragment extends Fragment {
    //string used to log error messages
    private static final String TAG = "FlagQuiz Activity";

    private static final  int FLAGS_IN_QUIZ = 10;

    private List<String> fileNameList; //list for the flag file names
    private List<String> quizCountriesList; //List for the countries in the current quiz
    private Set<String> regionsSet; //world regioms selected in the current quiz
    private String correctAnswer; //correct country for the current flag
    private int totalGuesses; //number of guesses made
    private int correctAnswers; //number of correct guesses
    private int guessRows; //number of rows to display the buttons
    private SecureRandom random; //used to randomize the quiz
    private Handler handler; //used to delay the next flag by some time
    private Animation shakeAnimation; //used to animate an incorrect guess

    private LinearLayout quizLinearLayout; //the quiz Layout
    private TextView questionNumbertextView; //textview for the current question number
    private ImageView flagImageView; //displays the flag
    private LinearLayout [] guessLinearLayouts; //rows for the answer buttons
    private TextView answerTextView; //displays the correct answer

    public MainActivityFragment() {
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        //load the shake animation used for an incorrect guess
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(),
                R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);

        //references to the GUI components
        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumbertextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);

        //configure the listeners for the guess buttons
        for (LinearLayout row: guessLinearLayouts){
            for (int column = 0; column < row.getChildCount(); column++){
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }
        //set the question number textview
        questionNumbertextView.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));

        //return the fragment's view for display
        return view;
    }


    //update the rows based on the value in the SharedPreferences
    public void updateGuessRows(SharedPreferences defaultSharedPreferences) {
        //get the number of guess buttons that should be shown
        String choices = defaultSharedPreferences.getString(MainActivity.CHOICES, null);
        guessRows = Integer.parseInt(choices) /2;

        //hide all the guess button in LinearLayouts
        for (LinearLayout layout : guessLinearLayouts)
            layout.setVisibility(View.GONE);

        //display the appropraite guess buttons based on the user setting
        for (int row = 0; row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
    }
    //update the regions based on the value on the SharedPreferences
    public void updateRegions(SharedPreferences defaultSharedPreferences) {
        regionsSet = defaultSharedPreferences.getStringSet(MainActivity.REGIONS, null);

    }

//set up and start the next quiz
    public void resetQuiz() {
        //Use the AssetMnager tio get the image file for the enabled regions
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear(); //enpty the list

        try {
            //loop through each region
            for (String region : regionsSet) {
                //get a list of all flag image files in this region
                String[] paths = assets.list(region);

                for (String path : paths)
                    fileNameList.add(path.replace(".png", ""));

            }
        }catch (IOException e){
            Log.e(TAG, "Error loading image file names", e);
        }

        correctAnswers = 0; //reset the number of correct answers
        totalGuesses = 0; //reset the total number of guesses
        quizCountriesList.clear(); //clear the countries list

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        //add FLAGS_IN_QUIZ random filenames to the QuizCountriesList
        while (flagCounter <= FLAGS_IN_QUIZ) {
            int randomIndex = random.nextInt(numberOfFlags);

            //get the random file
            String filename = fileNameList.get(randomIndex);

            //if the region is enabled and it has not already been chosen
            if(!quizCountriesList.contains(filename)){
                quizCountriesList.add(filename); //add the file to the list
                ++flagCounter;
            }

        }
        loadNextFlag();
    }

    //load the next flag after each guess or on reset
    private void loadNextFlag() {
        //get the file name of the next flag and remove it from the list
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage; //update the correct answer
        answerTextView.setText(""); //clear the text view

        //display the current question number
        questionNumbertextView.setText(getString(
                R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));

        //extract the region from the next image's name
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        //load the name of the next image with the AssetManager
        AssetManager assets = getActivity().getAssets();

        //get an InputStream to the asset representing the next flag
        //and try to use the InputStream
        final int version = Build.VERSION.SDK_INT; //get the current version of android running on the device
        try  {
            InputStream stream = assets.open(region + "/" + nextImage +".png");
            //load the asset as a drawable and show on the flagImageView
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);


            if (version >= 21) { //start animation for Lollipop Devices and above that support animation
                animate(false); //animate the flag unto the screen
            }
        } catch(IOException e){
            Log.e(TAG, "Error Loading " + nextImage, e);
        }
        Collections.shuffle(fileNameList); //shuffle the file names

        //put the correct answer at the end of the fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        //add 2,4,6 or 8 guess Buttons based on the value of guessRows
        for (int row = 0; row < guessRows; row++){
            //place Buttons in currentTableRow
            for (int column = 0;
                    column < guessLinearLayouts[row].getChildCount();
                    column++){
                //get reference to Button to configure
                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                //get the Country name and set it as the newGuessButton's text
                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getCountryName(filename));
            }
        }

        //place the correct answer in one Button randomly
        int row = random.nextInt(guessRows); //pick random row
        int column = random.nextInt(2); //pick random column
        LinearLayout randomRow = guessLinearLayouts[row]; //get the row
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }

    private String getCountryName(String filename) {
        return filename.substring(filename.indexOf('-') + 1).replace('_',' ');
    }

    private void animate(boolean animateOut) {
        //prevent animating the first flag
        if (correctAnswers == 0)
            return;

        //calculate center x and y
        int centerX = (quizLinearLayout.getLeft() + quizLinearLayout.getRight()) / 2;
        int centerY = (quizLinearLayout.getTop() + quizLinearLayout.getBottom()) / 2;

        //calculate animation radius
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());

        Animator animator;

        //if the layout should animate in or out
        if(animateOut){
            //create the circular reveal animation
            animator = ViewAnimationUtils.createCircularReveal(
                    quizLinearLayout, centerX, centerY, radius, 0);
            animator.addListener(
                    new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }
                    //called when the animation finishes
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            loadNextFlag();
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    }
            );
        }
        else{ //if the the layout should animate inwards
            animator = ViewAnimationUtils.createCircularReveal(
                    quizLinearLayout, centerX, centerY, 0, radius);
        }

        animator.setDuration(500); //animation duration set to 500ms
        animator.start(); //start the animation
    }
    //called when a button is clicked(touched)
    public View.OnClickListener guessButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
                Button guessButton = ((Button)v);
            String guess = guessButton.getText().toString();
            String answer =getCountryName(correctAnswer);
            ++totalGuesses; //increment the total number of guesses
            if (guess.equals(answer)){ //if the guess is correct
                ++correctAnswers; //increment the number of correct answers

                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(ContextCompat.getColor( getContext(),
                        R.color.correct_answer));
                
                disableButtons(); //disable all the buttons

                //if the user has correctly identified FLAGS_IN_QUIZ (set to 10) number of flags
                if (correctAnswers == FLAGS_IN_QUIZ){
                    //DialogFragment to display the quiz stats and start a new quiz
                    DialogFragment quizResults = new DialogFragment(){
                        //create an AlertDialog and return it
                        @Override
                        public Dialog onCreateDialog(Bundle bundle) {
                            AlertDialog.Builder builder =
                                    new AlertDialog.Builder(getActivity());
                            builder.setMessage(
                                    getString(R.string.results,
                                            totalGuesses,
                                            (1000 / (double) totalGuesses)));

                            // the Button to reset the quiz
                            builder.setPositiveButton(R.string.reset_quiz,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                                resetQuiz();
                                        }
                                    });
                            return builder.create();

                        }
                    };

                    //use the FragmentManager to diplay the DialogFragment
                    quizResults.setCancelable(false);
                   quizResults.show(getFragmentManager(), "quiz results");
                }
                else{ //answer is correct but quiz is not over
                    //load the next flag after a 2-second delay
                    handler.postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final int version = Build.VERSION.SDK_INT; //get the current version of android running on the device
                                    if (version >= 21) { //start animation for Lollipop Devices and above that support animation
                                        animate(true); //animate the flag off the screen
                                    } else{
                                        loadNextFlag();
                                    }
                                }
                            }, 2000); //2000ms delay

                }
            }
            else{//if answer is incorrect
                final int version = Build.VERSION.SDK_INT; //get the current version of android running on the device
                if(version >=21) { //start animation for Lollipop Devices and above that support animation
                    flagImageView.startAnimation(shakeAnimation);
                }
                //display "Incorrect Answer!" in red
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.incorrect_answer));
                guessButton.setEnabled(false);

            }
        }
    };

    private void disableButtons() {
        for (int row = 0; row < guessRows; row++){
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i = 0; i<guessRow.getChildCount(); i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }

}
