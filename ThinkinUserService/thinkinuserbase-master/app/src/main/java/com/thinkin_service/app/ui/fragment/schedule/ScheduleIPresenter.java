package com.thinkin_service.app.ui.fragment.schedule;

import com.thinkin_service.app.base.MvpPresenter;

import java.util.HashMap;

/**
 * Created by santhosh@appoets.com on 19-05-2018.
 */
public interface ScheduleIPresenter<V extends ScheduleIView> extends MvpPresenter<V> {
    void sendRequest(HashMap<String, Object> obj);
}
