import { useCallback, useState } from "react";
import { useNavigate, useRevalidator } from "react-router";
import { deleteDiary } from "../queries";

export function useDiaryActions(onDiaryDeleted?: () => void) {
  const navigate = useNavigate();
  const revalidator = useRevalidator();
  const [isDeleting, setIsDeleting] = useState<number | null>(null);

  const handleEdit = useCallback((id: number) => {
    navigate(`/diary/${id}/edit`);
  }, [navigate]);

  const handleDelete = useCallback(async (id: number) => {
    const confirmed = window.confirm("정말로 이 일기를 삭제하시겠습니까? 삭제된 일기는 복구할 수 없습니다.");
    
    if (!confirmed) return;

    try {
      setIsDeleting(id);
      await deleteDiary(id, "b0e0e902-3488-4c10-9621-fffde048923c"); // TODO: 실제 프로필 ID 사용
      
      // 목록 새로고침
      revalidator.revalidate();
      
      // 오늘 일기 버튼 새로고침
      onDiaryDeleted?.();
    } catch (error) {
      console.error("일기 삭제 중 오류:", error);
      alert("일기 삭제에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setIsDeleting(null);
    }
  }, [revalidator, onDiaryDeleted]);

  const handleView = useCallback((id: number) => {
    navigate(`/diary/${id}`);
  }, [navigate]);

  return {
    handleEdit,
    handleDelete,
    handleView,
    isDeleting,
  };
}