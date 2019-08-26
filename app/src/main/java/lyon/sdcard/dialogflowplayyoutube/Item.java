package lyon.sdcard.dialogflowplayyoutube;

public class Item {
    String TAG = Item.class.getSimpleName();
    public static final int SPEAKTOGOTYPE = 1;
    public static final int CUSTOMERTYPE = 2;
    int Type = -1;
    String sss = "";

    public int getType(){
        return Type;
    }

    public String getString(){
        return sss;
    }
}
