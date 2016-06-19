package fi.aalto.legroup.zop.sss;

import java.util.ArrayList;

/**
 * Created by khawar on 02/06/2016.
 */
public interface SSSEvent {
    public void processEvent(ArrayList<?> result);
    public void startEvent();
    public void stopEvent();
}
