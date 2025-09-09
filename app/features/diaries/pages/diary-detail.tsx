/**
 * 일기 상세 보기 페이지
 * 
 * 특정 일기의 모든 정보를 상세하게 표시합니다.
 * - 일기 작성 날짜 및 기본 정보
 * - 감정 태그들
 * - 작성된 모든 섹션 내용
 * - 작성 진행도
 */

import { Link, useParams } from "react-router";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "~/common/components/ui/breadcrumb";
import { Card, CardContent, CardHeader, CardTitle } from "~/common/components/ui/card";
import { Button } from "~/common/components/ui/button";
import { Badge } from "~/common/components/ui/badge";
import { 
  CalendarIcon, 
  EditIcon, 
  ArrowLeftIcon,
  CheckIcon 
} from "lucide-react";
import { cn } from "~/lib/utils";
import { getDiaryById } from "../queries";
import type { Route } from "./+types/diary-detail";
import { requireAuth } from "~/lib/auth.server";

export const loader = async ({ params, request }: { params: { id: string }, request: Request }) => {
  const { user } = await requireAuth(request);
  const profileId = user.id;
  const diaryId = parseInt(params.id);
  
  if (isNaN(diaryId)) {
    throw new Response("Invalid diary ID", { status: 400 });
  }

  const diary = await getDiaryById(diaryId, profileId);
  
  if (!diary) {
    throw new Response("Diary not found", { status: 404 });
  }

  return { diary };
};

export default function DiaryDetailPage({ loaderData }: Route.ComponentProps) {
  const { diary } = loaderData;
  const params = useParams();
  
  const {
    date,
    shortContent,
    situation,
    reaction,
    physicalSensation,
    desiredReaction,
    gratitudeMoment,
    selfKindWords,
    emotionTags,
    completedSteps,
    totalSteps,
  } = diary;

  const isComplete = completedSteps === totalSteps;
  const completionPercentage = totalSteps > 0 ? (completedSteps / totalSteps) * 100 : 0;

  const sections = [
    { title: "짧은 요약", content: shortContent, key: "shortContent" },
    { title: "상황", content: situation, key: "situation" },
    { title: "반응", content: reaction, key: "reaction" },
    { title: "신체 감각", content: physicalSensation, key: "physicalSensation" },
    { title: "원하는 반응", content: desiredReaction, key: "desiredReaction" },
    { title: "감사한 순간", content: gratitudeMoment, key: "gratitudeMoment" },
    { title: "자기 친화적 말", content: selfKindWords, key: "selfKindWords" },
  ];

  return (
    <div className="container mx-auto px-4 py-6 max-w-4xl">
      {/* 브레드크럼 */}
      <div className="mb-6">
        <Breadcrumb>
          <BreadcrumbList>
            <BreadcrumbItem>
              <BreadcrumbLink asChild>
                <Link to="/diary">일기</Link>
              </BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbPage>
                {date.toLocaleDateString("ko-KR", { 
                  year: "numeric",
                  month: "long", 
                  day: "numeric" 
                })}
              </BreadcrumbPage>
            </BreadcrumbItem>
          </BreadcrumbList>
        </Breadcrumb>
      </div>

      {/* 헤더 */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-3xl font-bold mb-2">
            {date.toLocaleDateString("ko-KR", {
              year: "numeric",
              month: "long",
              day: "numeric",
            })}
          </h1>
          <p className="text-muted-foreground flex items-center gap-2">
            <CalendarIcon className="w-4 h-4" />
            {date.toLocaleDateString("ko-KR", { weekday: "long" })}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" asChild>
            <Link to="/diary">
              <ArrowLeftIcon className="w-4 h-4 mr-2" />
              목록으로
            </Link>
          </Button>
          <Button asChild>
            <Link to={`/diary/${params.id}/edit`}>
              <EditIcon className="w-4 h-4 mr-2" />
              {isComplete ? "수정" : "계속 쓰기"}
            </Link>
          </Button>
        </div>
      </div>

      {/* 감정 태그 */}
      {emotionTags.length > 0 && (
        <div className="mb-6">
          <div className="flex flex-wrap gap-2">
            {emotionTags.map((tag) => (
              <Badge
                key={tag.id}
                style={{ backgroundColor: tag.color || "#6B7280" }}
                className="text-white"
              >
                {tag.name}
              </Badge>
            ))}
          </div>
        </div>
      )}

      {/* 작성 진행도 */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <div
              className={cn(
                "w-5 h-5 rounded-full flex items-center justify-center",
                isComplete ? "bg-green-500" : "bg-muted"
              )}
            >
              {isComplete && <CheckIcon className="w-3 h-3 text-white" />}
            </div>
            작성 진행도
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-4">
            <div className="flex-1">
              <div className="w-full bg-muted rounded-full h-3">
                <div
                  className="bg-primary h-3 rounded-full transition-all"
                  style={{ width: `${completionPercentage}%` }}
                />
              </div>
            </div>
            <span className="text-sm font-medium">
              {isComplete ? "완료" : `${completedSteps}/${totalSteps} 단계`} 
              ({Math.round(completionPercentage)}%)
            </span>
          </div>
        </CardContent>
      </Card>

      {/* 일기 내용 섹션들 */}
      <div className="space-y-6">
        {sections.map((section) => (
          section.content && (
            <Card key={section.key}>
              <CardHeader>
                <CardTitle className="text-lg">{section.title}</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="whitespace-pre-wrap leading-relaxed">
                  {section.content}
                </p>
              </CardContent>
            </Card>
          )
        ))}
      </div>

      {/* 작성된 내용이 없는 경우 */}
      {sections.every(section => !section.content) && (
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground mb-4">
              아직 작성된 내용이 없습니다.
            </p>
            <Button asChild>
              <Link to={`/diary/${params.id}/edit`}>
                <EditIcon className="w-4 h-4 mr-2" />
                일기 작성하기
              </Link>
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  );
}