// API route: POST /api/create-emotion-tag
import type { ActionFunctionArgs } from "react-router";
import { createCustomEmotionTag } from "~/features/emotions/queries";

export async function action({ request }: ActionFunctionArgs) {
  if (request.method !== "POST") {
    return Response.json({ error: "Method not allowed" }, { status: 405 });
  }

  try {
    const body = await request.json();
    const { profileId, name, color, category } = body;

    // Validate required fields
    if (!profileId || !name || !color || !category) {
      return Response.json(
        { error: "Missing required fields: profileId, name, color, category" },
        { status: 400 }
      );
    }

    // Validate category
    if (!["positive", "negative", "neutral"].includes(category)) {
      return Response.json(
        { error: "Invalid category. Must be 'positive', 'negative', or 'neutral'" },
        { status: 400 }
      );
    }

    const newTag = await createCustomEmotionTag(profileId, name, color, category);

    const formattedTag = {
      id: newTag.id,
      name: newTag.name,
      color: newTag.color || color,
      category: newTag.category || category,
      isDefault: newTag.isDefault || false,
    };

    return Response.json({ tag: formattedTag }, { status: 201 });
  } catch (error: any) {
    console.error("Error creating custom emotion tag:", error);
    
    // Handle specific error messages from createCustomEmotionTag
    if (error.message === "태그 이름을 입력해주세요." || 
        error.message === "이미 존재하는 태그 이름입니다.") {
      return Response.json({ error: error.message }, { status: 400 });
    }
    
    return Response.json(
      { error: "Failed to create custom emotion tag" }, 
      { status: 500 }
    );
  }
}