package com.usda.fmsc.twotrails.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.usda.fmsc.twotrails.R;

public class AnimationCardFragment extends Fragment {
    public static final String HIDDEN = "Hidden";

    private VisibilityListener listener;

    private boolean hidden, infocus;
    private View view;


    public AnimationCardFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();

        if (bundle != null) {
            if (bundle.containsKey(HIDDEN)) {
                hidden = getArguments().getBoolean(HIDDEN);
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.view = view;

        if (isCardHidden()) {
            if (this.view != null) {
                view.setY(-view.getHeight());
            }
        }

        if (infocus) {
            onCardFocused();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            if (isCardHidden()) {
                showCard();
            }

            infocus = true;

            if (view != null) {
                onCardFocused();
            }
        } else {
            infocus = false;

            if (view != null) {
                onCardUnFocused();
            }
        }
    }


    public void hideCard() {
        hideCard(true);
    }

    public void hideCard(boolean showAnimation) {
        if (view == null) {
            view = getView();
        }

        if (!hidden && view != null) {
            if (showAnimation) {
                Animation a = AnimationUtils.loadAnimation(getActivity(), R.anim.push_down_out_fast);

                if (listener != null) {
                    a.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            onCardHidden();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                }

                view.startAnimation(a);
            } else {
                view.setY(-view.getHeight());

                onCardHidden();
            }

            hidden = true;
        } else {
            onCardHidden();
        }
    }

    public void showCard() {
        showCard(true);
    }

    public void showCard(boolean showAnimation) {
        if (view == null) {
            view = getView();
        }

        if (hidden && view != null) {
            if (showAnimation) {
                Animation a = AnimationUtils.loadAnimation(getActivity(), R.anim.push_up_in_fast);

                if (listener != null) {
                    a.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            onCardVisible();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                }

                view.startAnimation(a);
            } else {
                view.setY(0);

                onCardVisible();
            }

            hidden = false;
        }
    }


    public boolean isCardHidden() {
        return hidden;
    }


    protected void onCardVisible() {
        if (listener != null)
            listener.onVisible();
    }

    protected void onCardHidden() {
        if (listener != null)
            listener.onHidden();
    }

    protected void onCardFocused() { }

    protected void onCardUnFocused() { }

    public void setVisibilityListener(VisibilityListener listener) {
        this.listener = listener;
    }


    public interface VisibilityListener {
        void onHidden();
        void onVisible();
    }
}
