// API route: GET /api/emotion-tags/:profileId
import type { LoaderFunctionArgs } from "react-router";
import { getEmotionTags } from "~/features/emotions/queries";

export async function loader({ params }: LoaderFunctionArgs) {
  const { profileId } = params;
  
  if (!profileId) {
    return Response.json({ error: "Profile ID is required" }, { status: 400 });
  }

  try {
    const tags = await getEmotionTags(profileId);
    
    const formattedTags = tags.map(tag => ({
      id: tag.id,
      name: tag.name,
      color: tag.color || "#6B7280",
      category: tag.category || "neutral",
      isDefault: tag.isDefault || false,
    }));

    return Response.json({ tags: formattedTags });
  } catch (error) {
    console.error("Error fetching emotion tags:", error);
    return Response.json(
      { error: "Failed to fetch emotion tags" }, 
      { status: 500 }
    );
  }
}