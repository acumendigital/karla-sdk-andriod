package co.getkarla.sdk.creditCardNfcReader.iso7816emv;

import co.getkarla.sdk.creditCardNfcReader.enums.TagTypeEnum;
import co.getkarla.sdk.creditCardNfcReader.enums.TagValueTypeEnum;


public interface ITag {

	enum Class {
		UNIVERSAL, APPLICATION, CONTEXT_SPECIFIC, PRIVATE
	}

	boolean isConstructed();

	byte[] getTagBytes();

	String getName();

	String getDescription();

	TagTypeEnum getType();

	TagValueTypeEnum getTagValueType();

	Class getTagClass();

	int getNumTagBytes();

}
