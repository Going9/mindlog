import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
  index("common/pages/landing.tsx"),
  route("diary", "features/diaries/pages/diary-list.tsx"),
  route("diary/new", "features/diaries/pages/new-diary.tsx"),
  route("diary/:id", "features/diaries/pages/diary-detail.tsx"),
  route("diary/:id/edit", "features/diaries/pages/edit-diary.tsx"),
] satisfies RouteConfig;
