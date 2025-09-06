import { useCallback } from "react";
import { useNavigate } from "react-router";

export function useDiaryActions() {
  const navigate = useNavigate();

  const handleEdit = useCallback((id: number) => {
    console.log("Edit entry:", id);
    // TODO: Navigate to edit page
  }, []);

  const handleDelete = useCallback((id: number) => {
    console.log("Delete entry:", id);
    // TODO: Show confirmation and delete
  }, []);

  const handleView = useCallback((id: number) => {
    navigate(`/diary/${id}`);
  }, [navigate]);

  return {
    handleEdit,
    handleDelete,
    handleView,
  };
}