package Applet2;

import javacard.framework.*;

public class Applet2 extends Applet
{
	final static byte INS_NHAP = (byte)0x00;
	final static byte INS_IN = (byte)0x01;
	final static byte INS_SUA = (byte)0x02;
	final static byte INS_XOA = (byte)0x03;
	final static byte INS_ARRAY = (byte)0x04;
	final static byte SV_ID_LENGTH = (byte)0x04;
	private static byte[] diemThi, sinhVien;
	private static byte soMonThi;

	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new Applet2(bArray,bOffset,bLength);
	}
	private Applet2(byte[] bArray, short bOffset, byte bLength)
	{
		//kiem tra du lieu den
		byte iLen = bArray[bOffset];//do dai AID
		if(iLen==0){
			//su dung AID mac dinh
			register();
		}
		else
		{
			//dang ki aid duoc chi dinh
			register(bArray,(short)(bOffset+1),iLen);
			bOffset = (short)(bOffset+iLen+1);
			byte cLen = bArray[bOffset];//do dai thong tin dieu khien
			bOffset = (short)(bOffset+cLen+1);
			byte aLen = bArray[bOffset];//do dai du lieu applet
			bOffset = (short)(bOffset+1);
			//doc du lieu applet
			if(aLen==0)
			{
				//gan Id cua sinh vien
				sinhVien = new byte[SV_ID_LENGTH];
				Util.arrayCopy(bArray,bOffset,sinhVien,(byte)0,SV_ID_LENGTH);
				bOffset += SV_ID_LENGTH;
				//gan mon thi
				soMonThi = bArray[bOffset];
			}
			else
			{
				//gan id sinh vien va so luong mon thi
				sinhVien = new  byte[]{'S','V','0','1'};
				soMonThi = (short)0;
				diemThi = new byte[(short)18];
			}
		}
	}

	public void process(APDU apdu)
	{
		if (selectingApplet())
		{
			return;
		}

		byte[] buf = apdu.getBuffer();
		apdu.setIncomingAndReceive();
		byte p1 = (byte)(buf[ISO7816.OFFSET_P1]);
		byte p2 = (byte)(buf[ISO7816.OFFSET_P2]);
		switch (buf[ISO7816.OFFSET_INS])
		{
		case INS_NHAP:
			if(soMonThi==(short)9){
				ISOException.throwIt(ISO7816.SW_BYTES_REMAINING_00);
				break;
			}
			
			for(short i = 0; i<soMonThi;i=(short)(i+2)){
				if(p1==diemThi[i]){
					ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
					break;
				}
			};
			byte[] diemNhap = JCSystem.makeTransientByteArray((short)2,JCSystem.CLEAR_ON_DESELECT);
			diemNhap[0]=p1;diemNhap[1]=p2;
			Util.arrayCopy(diemNhap,(short)0,diemThi,(short)(soMonThi*2),(short)2);
			soMonThi +=(short)1;
			break;
		case INS_IN:
			apdu.setOutgoing();
			apdu.setOutgoingLength((short)(soMonThi*2));
			apdu.sendBytesLong(diemThi,(short)0,(short)(soMonThi*2));
			break;
		case INS_SUA:
			boolean check = false;
			for(short i = 0; i<(short)(soMonThi*2);i=(short)(i+2)){
				if(p1==diemThi[i]){
					byte[] diemSua = JCSystem.makeTransientByteArray((short)2,JCSystem.CLEAR_ON_DESELECT);
					diemSua[0]=p1;diemSua[1]=p2;
					Util.arrayCopy(diemSua,(short)0,diemThi,i,(short)2);
					check = true;
					break;
				}
			};
			if(!check){
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
			}	
			break;
		case INS_XOA:
			boolean check2 = false;
			for(short i = 0; i<(short)(soMonThi*2);i=(short)(i+2)){
				if(p1==diemThi[i]){
					for(short j = i;j<(short)(soMonThi*2-2);j++){
						diemThi[j]= diemThi[(short)(j+2)];
					}
					diemThi[(short)((short)(soMonThi*2)-2)]=(byte)0x00;
					diemThi[(short)((short)(soMonThi*2)-1)]=(byte)0x00;
					soMonThi -=(short)1;
					check2 = true;
					break;
				}
			};
			if(!check2){
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
			}	
			break;	
		case INS_ARRAY:
			boolean check3 = true;
			byte[] arrDiem = JCSystem.makeTransientByteArray((short)18,JCSystem.CLEAR_ON_DESELECT);
			byte len = (byte)(buf[ISO7816.OFFSET_LC]);
			Util.arrayCopy(buf,ISO7816.OFFSET_CDATA,arrDiem,(short)0,len);
			//kiem tra so luong mon
			if((short)(len+soMonThi*2)>(short)18||(short)(len%2)!=0){
				ISOException.throwIt(ISO7816.SW_DATA_INVALID);
				break;
			}
			//kiem tra id mon
			for(short i = 0; i<(short)(len-2);i=(short)(i+2)){
				for(short j = (short)(i+2); j<len;j=(short)(j+2)){
					if(arrDiem[i]==arrDiem[j]){
						ISOException.throwIt(ISO7816.SW_DATA_INVALID);
						check3 = false;
						break;
					}
				}
				
				for(short k = 0;k<(short)(soMonThi*2);k=(short)(k+2)){
					if(arrDiem[i]==diemThi[k]){
						ISOException.throwIt(ISO7816.SW_DATA_INVALID);
						check3 = false;
						break;
					}
				}
				
				
			}
			if(check3){
				//luu mang
				Util.arrayCopy(buf,ISO7816.OFFSET_CDATA,diemThi,(short)(soMonThi*2),len);
				soMonThi +=(short)(len/2);
				//in ket qua
				apdu.setOutgoing();
				apdu.setOutgoingLength((short)(soMonThi*2));
				apdu.sendBytesLong(diemThi,(short)0,(short)(soMonThi*2));
			}
			
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
}