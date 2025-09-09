import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
  index("common/pages/landing.tsx"),
  
  // Auth routes
  route("login", "features/auth/pages/login.tsx"),
  route("auth/callback", "features/auth/pages/callback.tsx"),
  
  // Diary routes
  route("diary", "features/diaries/pages/diary-list.tsx"),
  route("diary/new", "features/diaries/pages/new-diary.tsx"),
  route("diary/:id", "features/diaries/pages/diary-detail.tsx"),
  route("diary/:id/edit", "features/diaries/pages/edit-diary.tsx"),
  
  // API routes
  route("api/emotion-tags/:profileId", "api/emotion-tags.$profileId.tsx"),
  route("api/create-emotion-tag", "api/create-emotion-tag.tsx"),
  route("api/delete-emotion-tag", "api/delete-emotion-tag.tsx"),
] satisfies RouteConfig;
