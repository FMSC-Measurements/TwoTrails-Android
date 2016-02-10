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

    private boolean hidden;
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

        if (hidden) {
            if (view == null) {
                this.view = getView();
            }

            if (this.view != null) {
                view.setY(-view.getHeight());
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser && isCardHidden()) {
            showCard();
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
                            listener.onHidden();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                }

                view.startAnimation(a);
            } else {
                view.setY(-view.getHeight());

                if (listener != null) {
                    listener.onHidden();
                }
            }

            hidden = true;
        } else {
            if (listener != null) {
                listener.onHidden();
            }
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
                            listener.onVisible();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                }

                view.startAnimation(a);
            } else {
                view.setY(0);

                if (listener != null) {
                    listener.onVisible();
                }
            }

            hidden = false;
        }
    }


    public boolean isCardHidden() {
        return hidden;
    }


    public void setVisibilityListener(VisibilityListener listener) {
        this.listener = listener;
    }


    public interface VisibilityListener {
        void onHidden();
        void onVisible();
    }
}
