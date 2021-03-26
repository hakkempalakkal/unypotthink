package com.thinkin_service.provider.ui.fragment.incoming_request;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.support.v7.widget.AppCompatRatingBar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.thinkin_service.provider.BuildConfig;
import com.thinkin_service.provider.R;
import com.thinkin_service.provider.base.BaseFragment;
import com.thinkin_service.provider.common.Constants;
import com.thinkin_service.provider.common.Utilities;
import com.thinkin_service.provider.data.network.model.Request_;
import android.support.v4.app.Fragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.StringTokenizer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import de.hdodenhof.circleimageview.CircleImageView;
import es.dmoral.toasty.Toasty;

import static com.thinkin_service.provider.MvpApplication.DATUM;
import static com.thinkin_service.provider.MvpApplication.mLastKnownLocation;
import static com.thinkin_service.provider.MvpApplication.time_to_left;

public class IncomingRequestFragment extends BaseFragment implements IncomingRequestIView {

    @BindView(R.id.btnReject)
    Button btnReject;
    @BindView(R.id.btnAccept)
    Button btnAccept;
    Unbinder unbinder;
    @BindView(R.id.lblCount)
    TextView lblCount;
    @BindView(R.id.imgUser)
    CircleImageView imgUser;
    @BindView(R.id.lblUserName)
    TextView lblUserName;
    @BindView(R.id.ratingUser)
    AppCompatRatingBar ratingUser;
    @BindView(R.id.lblLocationName)
    TextView lblLocationName;
    @BindView(R.id.DestLocationName)
    TextView DestLocationName;
    @BindView(R.id.lblDrop)
    TextView lblDrop;
    @BindView(R.id.lblScheduleDate)
    TextView lblScheduleDate;
    @BindView(R.id.Distance)
    TextView Distance;
    @BindView(R.id.PaymentType)
    TextView PaymentType;
    @BindView(R.id.DistanceUser)
    TextView DistanceUser;
    @BindView(R.id.EstimatedFare)
    TextView EstimatedFare;

    private IncomingRequestPresenter presenter = new IncomingRequestPresenter();
    private Context context;
    public static CountDownTimer countDownTimer;
    public static MediaPlayer mPlayer;
//    private Double estimatedFare;

    @Override
    public Fragment fragmentInstance() {
        return IncomingRequestFragment.this;
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_incoming_request;
    }

    @Override
    public View initView(View view) {
        unbinder = ButterKnife.bind(this, view);
        context = getContext();
        presenter.attachView(this);
        mPlayer = MediaPlayer.create(context, R.raw.alert_tone);
        init();
        return view;
    }

    @SuppressLint("SetTextI18n")
    void init() {
        Request_ data = DATUM;
        if (data != null) {
            lblUserName.setText(String.format("%s %s", data.getUser().getFirstName(),
                    data.getUser().getLastName()));
            ratingUser.setRating(Float.parseFloat(data.getUser().getRating()));
            if (data.getSAddress() != null && !data.getSAddress().isEmpty()
                    || data.getDAddress() != null && !data.getDAddress().isEmpty()) {
                lblLocationName.setText(data.getSAddress());
                DestLocationName.setText(data.getDAddress());
                Distance.setText(String.valueOf(data.getDistance()) + "Km");
                Log.e("serviiiice", String.valueOf(DATUM));
                EstimatedFare.setText(String.format("%s",data.getServiceType().getName()));
                lblDrop.setText(data.getDAddress());

                //TODO ALLAN - Alterações débito na máquina e voucher
                if(data.getPaymentMode().equals("CASH")){
                    PaymentType.setText("CASH");
                }else if(data.getPaymentMode().equals("CARD")){
                    PaymentType.setText("CARD");
                }else if(data.getPaymentMode().equals("DEBIT_MACHINE")){
                    PaymentType.setText("Debit Machine");
                }else{
                    PaymentType.setText(data.getPaymentMode());
                }

                //Seta distância entre motorista e passageiro
                Double distUser = getDistancia(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude(), data.getSLatitude(), data.getSLongitude());
                DistanceUser.setText(String.format(Locale.getDefault(), "%.2f", distUser) + "Km");

            }
            if (data.getUser().getPicture() != null)
                Glide.with(activity()).load(BuildConfig.BASE_IMAGE_URL + data.getUser()
                        .getPicture()).apply(RequestOptions.placeholderOf(R.drawable.ic_user_placeholder)
                        .dontAnimate().error(R.drawable.ic_user_placeholder)).into(imgUser);
        }

        String isScheduled = data.getIsScheduled();
        String scheduledAt = data.getScheduleAt();
        if (isScheduled != null && isScheduled.equalsIgnoreCase("NO")) {
            lblScheduleDate.setVisibility(View.INVISIBLE);
        } else {
            if (scheduledAt != null && isScheduled.equalsIgnoreCase("YES")) {
                StringTokenizer tk = new StringTokenizer(scheduledAt);
                String date = tk.nextToken();
                String time = tk.nextToken();
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat sdfs = new SimpleDateFormat("hh:mm a");
                Date dt;
                try {
                    dt = sdf.parse(time);
                    lblScheduleDate.setVisibility(View.VISIBLE);
                    lblScheduleDate.setText(getString(R.string.schedule_for) + " " +
                            Utilities.convertDate(scheduledAt) + " " + sdfs.format(dt));
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }
        countDownTimer = new CountDownTimer(time_to_left * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                lblCount.setText(String.valueOf(millisUntilFinished / 1000));
                setTvZoomInOutAnimation(lblCount);
            }

            public void onFinish() {
                try {
                    context.sendBroadcast(new Intent("INTENT_FILTER"));
                    mPlayer.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        countDownTimer.start();
    }


    /**
     * CALCULA DISTÂNCIA ENTRE 2 PONTOS
     */
    public static double getDistancia(Double lat1, Double long1, Double lat2, Double long2) {
        // Conversão de graus pra radianos das latitudes
        double firstLatToRad = Math.toRadians(lat1);
        double secondLatToRad = Math.toRadians(lat2);

        // Diferença das longitudes
        double deltaLongitudeInRad = Math.toRadians(long2 - long1);

        // Cálcula da distância entre os pontos
        return Math.acos(Math.cos(firstLatToRad) * Math.cos(secondLatToRad)
                * Math.cos(deltaLongitudeInRad) + Math.sin(firstLatToRad)
                * Math.sin(secondLatToRad))
                * 6371;
    }


    private void setTvZoomInOutAnimation(final TextView textView) {
        final float startSize = 20;
        final float endSize = 13;
        final int animationDuration = 900; // Animation duration in ms

        ValueAnimator animator = ValueAnimator.ofFloat(startSize, endSize);
        animator.setDuration(animationDuration);

        animator.addUpdateListener(valueAnimator -> {
            float animatedValue = (Float) valueAnimator.getAnimatedValue();
            textView.setTextSize(animatedValue);
        });
        //animator.setRepeatCount(ValueAnimator.INFINITE);  // Use this line for infinite animations
        animator.setRepeatCount(2);
        animator.start();
    }


    @OnClick({R.id.btnReject, R.id.btnAccept})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btnReject:
                if (DATUM != null) {
                    Request_ datum = DATUM;
                    showLoading();
                    presenter.cancel(datum.getId());
                    time_to_left = 60;
                }
                break;
            case R.id.btnAccept:
                if (DATUM != null) {
                    Request_ datum = DATUM;
                    showLoading();
                    presenter.accept(datum.getId());
                    time_to_left = 60;
                }
                break;
        }
    }

    @Override
    public void onSuccessAccept(Object message) {
        countDownTimer.cancel();
        hideLoading();

        if(message.toString().equals("{error = Time out!}")){
            Toasty.error(Objects.requireNonNull(getContext()),"Timeout. Call ended!", Toast.LENGTH_LONG).show();
        } else if(message.toString().equals("{error = Trip accepted by another driver!}")){
            Toasty.error(getContext(),"Trip accepted by another driver!", Toast.LENGTH_LONG).show();
        }else{
            Toasty.success(getContext(), getString(R.string.ride_accepted), Toast.LENGTH_LONG).show();
        }

        getContext().sendBroadcast(new Intent("INTENT_FILTER"));
        try {
            getActivity().getSupportFragmentManager().beginTransaction().remove(IncomingRequestFragment.this).commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSuccessCancel(Object object) {
        countDownTimer.cancel();
        hideLoading();
        getActivity().getSupportFragmentManager().beginTransaction().remove(IncomingRequestFragment.this).commitAllowingStateLoss();
        Toasty.success(context, getString(R.string.ride_cancelled), Toast.LENGTH_SHORT, true).show();
        context.sendBroadcast(new Intent("INTENT_FILTER"));
    }

    @Override
    public void onError(Throwable e) {
        try {
            hideLoading();
            if (mPlayer.isPlaying()) mPlayer.stop();
            if (e != null)
                onErrorBase(e);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mPlayer.isPlaying())
            mPlayer.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPlayer.isPlaying())
            mPlayer.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPlayer.isPlaying())
            mPlayer.stop();
    }
}
