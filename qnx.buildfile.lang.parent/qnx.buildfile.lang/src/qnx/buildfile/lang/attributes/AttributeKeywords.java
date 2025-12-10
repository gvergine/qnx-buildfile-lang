package qnx.buildfile.lang.attributes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AttributeKeywords
{
	public final static List<String> MKIFS_ATTRIBUTE_KEYWORDS;
	public final static List<String> MKQNX6FS_ATTRIBUTE_KEYWORDS;
	public final static List<String> ALL_ATTRIBUTE_KEYWORDS;

	static
	{
		MKIFS_ATTRIBUTE_KEYWORDS = Stream.concat(
				Arrays.stream(MkifsBooleanAttributeKeyword.values()).map(Enum::name),
				Arrays.stream(MkifsValuedAttributeKeyword.values()).map(Enum::name)
				)
				.distinct()
				.collect(Collectors.toUnmodifiableList());

		MKQNX6FS_ATTRIBUTE_KEYWORDS = Stream.concat(
				Arrays.stream(Mkqnx6fsBooleanAttributeKeyword.values()).map(Enum::name),
				Arrays.stream(Mkqnx6fsValuedAttributeKeyword.values()).map(Enum::name)
				)
				.distinct()
				.collect(Collectors.toUnmodifiableList());


		ALL_ATTRIBUTE_KEYWORDS = Stream.concat(
				Arrays.stream(UndocumentedValuedAttributeKeyword.values()).map(Enum::name),
				Stream.concat(
						MKIFS_ATTRIBUTE_KEYWORDS.stream(),
						MKQNX6FS_ATTRIBUTE_KEYWORDS.stream())
				).distinct().collect(Collectors.toUnmodifiableList());

	}

}
