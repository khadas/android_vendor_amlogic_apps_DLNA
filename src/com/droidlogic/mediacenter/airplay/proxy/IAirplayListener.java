package com.droidlogic.mediacenter.airplay.proxy;

public interface IAirplayListener
{
    public void onNetworkStateChange ( boolean connect );
    
    public void onAirplayDeviceStateChange();
    
    public void onAirplayPlayStateChange();
    
    public void onStorageStateChange();
}
