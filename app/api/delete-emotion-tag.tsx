// API route: DELETE /api/delete-emotion-tag
import type { ActionFunctionArgs } from "react-router";
import { deleteCustomEmotionTag } from "~/features/emotions/queries";

export async function action({ request }: ActionFunctionArgs) {
  if (request.method !== "DELETE") {
    return Response.json({ error: "Method not allowed" }, { status: 405 });
  }

  try {
    const body = await request.json();
    const { profileId, tagId } = body;

    // Validate required fields
    if (!profileId || !tagId) {
      return Response.json(
        { error: "Missing required fields: profileId, tagId" },
        { status: 400 }
      );
    }

    // Validate tagId is a number
    const numericTagId = parseInt(tagId);
    if (isNaN(numericTagId)) {
      return Response.json(
        { error: "tagId must be a valid number" },
        { status: 400 }
      );
    }

    const deletedTag = await deleteCustomEmotionTag(profileId, numericTagId);

    return Response.json({ 
      success: true,
      deletedTag: {
        id: deletedTag.id,
        name: deletedTag.name,
      }
    }, { status: 200 });
  } catch (error: any) {
    console.error("Error deleting custom emotion tag:", error);
    
    // Handle specific error messages from deleteCustomEmotionTag
    if (error.message === "존재하지 않는 태그입니다." || 
        error.message === "기본 태그는 삭제할 수 없습니다." ||
        error.message === "다른 사용자의 태그는 삭제할 수 없습니다.") {
      return Response.json({ error: error.message }, { status: 400 });
    }
    
    return Response.json(
      { error: "Failed to delete custom emotion tag" }, 
      { status: 500 }
    );
  }
}