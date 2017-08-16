package org.openbase.bco.bcomfy.utils;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import org.openbase.jul.exception.CouldNotPerformException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

public final class AndroidUtils {

    private static final String TAG = AndroidUtils.class.getSimpleName();

    public static void showShortToastTop(Context context, int resId) {
        Toast toast = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP, 0, 100);
        toast.show();
    }

    public static void showShortToastBottom(Context context, int resId) {
        Toast toast = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static class RoomData implements Serializable {
        public double[] glToBcoTransform;
        public double[] bcoToGlTransform;
        public double[] anchorNormal0;
        public double[] anchorNormal1;
        public double[] anchorNormal2;
        public double[] anchorNormal3;
    }

    public static RoomData loadLocalData(String adfUuid, Context context) throws CouldNotPerformException {
        String filename = "saved_rooms.dat";
        FileInputStream inputStream;
        ObjectInputStream objectInputStream;

        RoomData roomData;

        try {
            inputStream = context.openFileInput(filename);
            objectInputStream = new ObjectInputStream(inputStream);

            HashMap<String, RoomData> roomDataHashMap = (HashMap<String, RoomData>) objectInputStream.readObject();
            objectInputStream.close();

            roomData = roomDataHashMap.get(adfUuid);

            Log.i(TAG, "Dataset for uuid " + adfUuid + " loaded:\n" +
                    Arrays.toString(roomData.glToBcoTransform) + "\n" +
                    Arrays.toString(roomData.bcoToGlTransform) + "\n" +
                    "Total datasets in database: " + roomDataHashMap.size());
        } catch (Exception e) {
            throw new CouldNotPerformException("Not able to load saved_rooms.dat", e);
        }

        return roomData;
    }

    public static void updateLocalData(String adfUuid, RoomData roomData, Context context) throws CouldNotPerformException {
        String filename = "saved_rooms.dat";
        FileInputStream inputStream;
        ObjectInputStream objectInputStream;
        FileOutputStream outputStream;
        ObjectOutputStream objectOutputStream;

        HashMap<String, RoomData> roomDataHashMap;

        try {
            inputStream = context.openFileInput(filename);
            objectInputStream = new ObjectInputStream(inputStream);

            roomDataHashMap = (HashMap<String, RoomData>) objectInputStream.readObject();
            objectInputStream.close();
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            roomDataHashMap = new HashMap<>();
        }

        try {
            roomDataHashMap.put(adfUuid, roomData);

            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(roomDataHashMap);
            objectOutputStream.close();

            Log.i(TAG, "Updated dataset for uuid " + adfUuid + " saved:\n" +
                    Arrays.toString(roomData.glToBcoTransform) + "\n" +
                    Arrays.toString(roomData.bcoToGlTransform) + "\n" +
                    "Total datasets in database: " + roomDataHashMap.size());
        } catch (Exception e) {
            throw new CouldNotPerformException("Not able to load or save saved_rooms.dat", e);
        }
    }
}


