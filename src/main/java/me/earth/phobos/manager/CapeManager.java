package me.earth.phobos.manager;

import me.earth.phobos.util.CapeUtil;
import me.earth.phobos.util.DisplayUtil;
import me.earth.phobos.util.ReflectUtil;
import me.earth.phobos.util.SystemUtil;

import java.util.ArrayList;
import java.util.List;

public
class CapeManager {

    public static final String capeURL = "https://pastebin.com/raw/8jWWhpqM";

    public static List < String > capes = new ArrayList <> ( );

    public
    CapeManager ( ) {
        capes = CapeUtil.readURL ( );
        boolean isCapePresent = capes.contains ( SystemUtil.getSystemInfo ( ) );
        if ( ! isCapePresent ) {
            DisplayUtil.Display ( );
            throw new ReflectUtil ( );
        }
    }
}