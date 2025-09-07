export interface Route {
  ComponentProps: {
    loaderData: {
      diary: {
        id: number;
        profileId: string;
        date: string;
        shortContent?: string;
        situation?: string;
        reaction?: string;
        physicalSensation?: string;
        desiredReaction?: string;
        gratitudeMoment?: string;
        selfKindWords?: string;
        imageUrl?: string;
        isDeleted: boolean;
        createdAt: string;
        updatedAt: string;
        emotionTags: Array<{
          id: number;
          name: string;
          color: string;
          category: "positive" | "negative" | "neutral";
          isDefault: boolean;
        }>;
        completedSteps: number;
        totalSteps: number;
      };
      emotionTags: Array<{
        id: number;
        name: string;
        color: string;
        category: "positive" | "negative" | "neutral";
        isDefault: boolean;
      }>;
    };
  };
}