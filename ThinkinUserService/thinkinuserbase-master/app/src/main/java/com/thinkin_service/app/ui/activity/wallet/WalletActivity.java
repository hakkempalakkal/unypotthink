package com.thinkin_service.app.ui.activity.wallet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.appoets.braintreepayment.BrainTreePaymentActivity;
import com.appoets.paytmpayment.PaytmCallback;
import com.appoets.paytmpayment.PaytmObject;
import com.appoets.paytmpayment.PaytmPayment;
import com.thinkin_service.app.R;
import com.thinkin_service.app.base.BaseActivity;
import com.thinkin_service.app.common.Constants;
import com.thinkin_service.app.data.SharedHelper;
import com.thinkin_service.app.data.network.model.AddWallet;
import com.thinkin_service.app.data.network.model.BrainTreeResponse;
import com.thinkin_service.app.data.network.model.Card;
import com.thinkin_service.app.data.network.model.Plan;
import com.thinkin_service.app.data.network.model.Wallet;
import com.thinkin_service.app.ui.activity.card.CardsActivity;
import com.thinkin_service.app.ui.activity.payment.PaymentActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.dmoral.toasty.Toasty;

import static android.text.TextUtils.isEmpty;
import static com.thinkin_service.app.MvpApplication.isBraintree;
import static com.thinkin_service.app.MvpApplication.isCard;
import static com.thinkin_service.app.MvpApplication.isPaytm;
import static com.thinkin_service.app.MvpApplication.isPayumoney;
import static com.thinkin_service.app.ui.activity.payment.PaymentActivity.PICK_PAYMENT_METHOD;

public class WalletActivity extends BaseActivity implements WalletIView {

    @BindView(R.id.wallet_balance)
    TextView walletBalance;
    @BindView(R.id.amount)
    EditText amount;
    @BindView(R.id._199)
    Button _199;
    @BindView(R.id._599)
    Button _599;
    @BindView(R.id._1099)
    Button _1099;
    @BindView(R.id.add_amount)
    Button addAmount;
    @BindView(R.id.cvAddMoneyContainer)
    CardView cvAddMoneyContainer;
    String regexNumber = "^(\\d{0,9}\\.\\d{1,4}|\\d{1,9})$";
    private WalletPresenter<WalletActivity> presenter = new WalletPresenter<>();

    private static final int BRAINTREE_PAYMENT_REQUEST_CODE = 101;


    @Override
    public int getLayoutId() {
        return R.layout.activity_wallet;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void initView() {

        ButterKnife.bind(this);
        presenter.attachView(this);
        // Activity title will be updated after the locale has changed in Runtime
        setTitle(getString(R.string.wallet));

        _199.setText(SharedHelper.getKey(this, "currency") + " " + getString(R.string._199));
        _599.setText(SharedHelper.getKey(this, "currency") + " " + getString(R.string._599));
        _1099.setText(SharedHelper.getKey(this, "currency") + " " + getString(R.string._1099));
        amount.setTag(SharedHelper.getKey(this, "currency"));

        walletBalance.setText(SharedHelper.getKey(this, "currency") + " " + Double.parseDouble(SharedHelper.getKey(this, "walletBalance", "0")));

        if (!isCard && !isBraintree && !isPaytm && !isPayumoney) {
            cvAddMoneyContainer.setVisibility(View.GONE);
            addAmount.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @OnClick({R.id._199, R.id._599, R.id._1099, R.id.add_amount})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id._199:
                amount.setText(getString(R.string._199));
                break;
            case R.id._599:
                amount.setText(getString(R.string._599));
                break;
            case R.id._1099:
                amount.setText(getString(R.string._1099));
                break;
            case R.id.add_amount:
                if (!amount.getText().toString().trim().matches(regexNumber)) {
                    Toast.makeText(baseActivity(), getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(baseActivity(), PaymentActivity.class);
                intent.putExtra("hideCash", true);
                startActivityForResult(intent, PICK_PAYMENT_METHOD);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_PAYMENT_METHOD && resultCode == Activity.RESULT_OK && data != null)
            switch (data.getStringExtra("payment_mode")) {
                case Constants.PaymentMode.CARD:
                    HashMap<String, Object> map = new HashMap<>();
                    String cardId = data.getStringExtra("card_id");
                    map.put("amount", amount.getText().toString());
                    map.put("payment_mode", "CARD");
                    map.put("card_id", cardId);
                    map.put("user_type", "user");
                    showLoading();
                    presenter.addMoney(map);
                    break;
                case Constants.PaymentMode.BRAINTREE:
                    presenter.getBrainTreeToken();
                    break;
                case Constants.PaymentMode.PAYTM: {
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("amount", amount.getText().toString());
                    hashMap.put("payment_mode", Constants.PaymentMode.PAYTM);
                    hashMap.put("user_type", "user");
                    showLoading();
                    presenter.addMoneyPaytm(hashMap);
                    break;
                }
            }
        else if (requestCode == BRAINTREE_PAYMENT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                HashMap<String, Object> map = new HashMap<>();
                String nonce = data.getStringExtra(BrainTreePaymentActivity.PAYMENT_NONCE);
                map.put("amount", amount.getText().toString());
                map.put("payment_mode", "BRAINTREE");
                map.put("braintree_nonce", nonce);
                map.put("user_type", "user");
                showLoading();
                presenter.addMoney(map);
            } else
                Toasty.error(WalletActivity.this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onSuccess(PaytmObject object) {

        try {
            hideLoading();
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        new PaytmPayment(WalletActivity.this, object, new PaytmCallback() {
            @Override
            public void onPaytmSuccess(String status, String message, String paymentmode, String txid) {
                Toasty.success(WalletActivity.this, "Value added successfully!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPaytmFailure(String errorMessage) {
                Toasty.error(WalletActivity.this,
                        "Failed to add value", Toast.LENGTH_SHORT).show();
            }
        }).startPayment();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSuccess(AddWallet wallet) {
        try {
            hideLoading();
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        if(wallet.getMessage().equals("Transaction Failed")){
            Toast.makeText(this, "\n" +
                    "Card failed or insufficient balance! Please try again.", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this, wallet.getMessage(), Toast.LENGTH_LONG).show();
        }

        amount.setText("");

        SharedHelper.putKey(this, "walletBalance", String.valueOf(wallet.getBalance()));
        walletBalance.setText(SharedHelper.getKey(this, "currency") + " " + Double.parseDouble(SharedHelper.getKey(this, "walletBalance", "0")));

        //walletBalance.setText(getNumberFormat().format(Double.parseDouble(SharedHelper.getKey(this, "walletBalance", "0"))));
    }

    @Override
    public void onSuccess(BrainTreeResponse response) {

        if (!response.getToken().isEmpty()) {
            Intent intent = new Intent(WalletActivity.this, BrainTreePaymentActivity.class);
            intent.putExtra(BrainTreePaymentActivity.EXTRAS_TOKEN, response.getToken());
            startActivityForResult(intent, BRAINTREE_PAYMENT_REQUEST_CODE);
        }

    }

    @Override
    public void onError(Throwable e) {
        handleError(e);
    }

}
