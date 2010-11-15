/* This file is part of "MidpSSH".
 * Copyright (c) 2005 Karl von Randow.
 * 
 * --LICENSE NOTICE--
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * --LICENSE NOTICE--
 *
 */
package app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;

/**
 * @author Karl
 * 
 */
public abstract class MyRecordStore {

    protected Vector load(String rmsName, boolean sort) {
        try {
            RecordStore rec = RecordStore.openRecordStore(rmsName, false);

            RecordEnumeration recs = rec.enumerateRecords(null, null, false);
            Vector vector = new Vector();

            while (recs.hasNextElement()) {
                byte[] data = recs.nextRecord();
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
                try {
                    vector.addElement(read(in));
                } catch (IOException e) {
                    
                }
                in.close();
            }
            rec.closeRecordStore();
            
//#ifndef small
            if (sort) {
                insertSort(vector);
            }
//#endif
            return vector;
        } catch (Exception e) {
            
        }
        return new Vector();
    }

    protected void insertSort(Vector v) {
        int in, out;
        int n = v.size();

        for (out = 1; out < n; out++) // out is dividing line
        {
            Object temp = v.elementAt(out);
            in = out;
            while (in > 0 && compare(v.elementAt(in - 1), temp) >= 0) {
                v.setElementAt(v.elementAt(in - 1), in);
                --in;
            }
            v.setElementAt(temp, in);
        }
    }
    
    protected int compare(Object a, Object b) {
        return 0;
    }

    protected void save(String rmsName, Vector vector) {
        if (vector != null) {
            try {
                try {
                    RecordStore.deleteRecordStore(rmsName);
                } catch (Exception e) {

                }

                RecordStore rec = RecordStore.openRecordStore(rmsName, true);
                for (int i = 0; i < vector.size(); i++) {
                    Object ob = vector.elementAt(i);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    DataOutputStream dout = new DataOutputStream(out);
                    try {
                        write(dout, ob);
                        dout.close();

                        byte[] data = out.toByteArray();
                        rec.addRecord(data, 0, data.length);
                    } catch (IOException e) {
                        
                    }
                }

                rec.closeRecordStore();
            } catch (Exception e) {

            }
        }
    }

    protected abstract Object read(DataInputStream in) throws IOException;

    protected abstract void write(DataOutputStream out, Object ob) throws IOException;
}
