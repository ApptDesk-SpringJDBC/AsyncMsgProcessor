package com.ca.asynchmsg.iso;

import java.util.BitSet;

import org.jpos.iso.ISOBasePackager;
import org.jpos.iso.ISOBitMap;
import org.jpos.iso.ISOBitMapPackager;
import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;

import com.ca.asynchmsg.message.MessageUIDGenerator;

public class CAISOBasePackager extends ISOBasePackager{
	MessageUIDGenerator gen = null;
	public void setMsgFieldUIDGen(MessageUIDGenerator gen){
		this.gen = gen;
	}
	/**
     * @param   m   the Container of this message
     * @param   b   ISO message image
     * @return      consumed bytes
     * @exception ISOException
     */
    public int unpack (ISOComponent m, byte[] b) throws ISOException {
        LogEvent evt = logger != null ?  new LogEvent (this, "unpack") : null;
        int consumed = 0;

        try {
            if (m.getComposite() != m) 
                throw new ISOException ("Can't call packager on non Composite");
            if (evt != null)  // save a few CPU cycle if no logger available
                evt.addMessage (ISOUtil.hexString (b));

            
            // if ISOMsg and headerLength defined 
            if (m instanceof ISOMsg /*&& ((ISOMsg) m).getHeader()==null*/ && headerLength>0) 
            {
            	byte[] h = new byte[headerLength];
                System.arraycopy(b, 0, h, 0, headerLength);
            	((ISOMsg) m).setHeader(h);
            	consumed += headerLength;
            }       
            
            if (!(fld[0] == null) && !(fld[0] instanceof ISOBitMapPackager))
            {
                ISOComponent mti = fld[0].createComponent(0);
                consumed  += fld[0].unpack(mti, b, consumed);
                m.set (mti);
            }
            BitSet bmap = null;
            //int maxField = fld.length;
            int maxField = 0;
            if (emitBitMap()) {
                ISOBitMap bitmap = new ISOBitMap (-1);
                consumed += getBitMapfieldPackager().unpack(bitmap,b,consumed);
                bmap = (BitSet) bitmap.getValue();
                if (evt != null)
                    evt.addMessage ("<bitmap>"+bmap.toString()+"</bitmap>");
                m.set (bitmap);
                //maxField = Math.min(maxField, bmap.size());                
            }
            //Added to find out maximum UID from JSON file.
            /*String[] uid = ATMSwitchTCPHandler.msgUIDFormat.split(",");
            maxField = (Math.max(Integer.parseInt(uid[0]), Integer.parseInt(uid[1])))+1;*/
            
            for(int field : gen.getISOMsgFields()){
            		maxField = Math.max(maxField,field);
            }
            maxField = maxField+1;
            for (int i=getFirstField(); i<maxField; i++) {
                try {
                    if (bmap == null && fld[i] == null)
                        continue;
                    if (maxField > 128 && i==65)
                        continue;   // ignore extended bitmap

                    if (bmap == null || bmap.get(i)) {
                        if (fld[i] == null)
                            throw new ISOException ("field packager '" + i + "' is null");

                        ISOComponent c = fld[i].createComponent(i);
                        consumed += fld[i].unpack (c, b, consumed);
                        if (evt != null) {
                            evt.addMessage ("<unpack fld=\"" + i 
                                +"\" packager=\""
                                +fld[i].getClass().getName()+ "\">");
                            if (c.getValue() instanceof ISOMsg)
                                evt.addMessage (c.getValue());
                            else if (c.getValue() instanceof byte[]) {
                                evt.addMessage ("  <value type='binary'>" 
                                    +ISOUtil.hexString((byte[]) c.getValue())
                                    + "</value>");
                            }
                            else {
                                evt.addMessage ("  <value>" 
                                    +c.getValue()
                                    + "</value>");
                            }
                            evt.addMessage ("</unpack>");
                        }
                        m.set(c);
                    }
                } catch (ISOException e) {
                    if (evt != null) {
                        evt.addMessage(
                                "error unpacking field " + i + " consumed=" + consumed
                        );
                        evt.addMessage(e);
                    }
                    // jPOS-3
                    e = new ISOException (
                        String.format ("%s (%s) unpacking field=%d, consumed=%d",
                        e.getMessage(), e.getNested().toString(), i, consumed)
                    );
                    throw e;
                }
            }
            if (evt != null && b.length != consumed) {
                evt.addMessage (
                    "WARNING: unpack len=" +b.length +" consumed=" +consumed
                );
            }
            return consumed;
        } catch (ISOException e) {
            if (evt != null)
                evt.addMessage (e);
            throw e;
        } catch (Exception e) {
            if (evt != null)
                evt.addMessage (e);
            throw new ISOException (e.getMessage() + " consumed=" + consumed);
        } finally {
            if (evt != null)
                Logger.log (evt);
        }
    }
	
}
