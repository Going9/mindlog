ALTER TABLE "diaries" DROP CONSTRAINT "diaries_profile_id_date_is_deleted_unique";--> statement-breakpoint
ALTER TABLE "diaries" ADD COLUMN "deleted_at" timestamp with time zone;--> statement-breakpoint
CREATE UNIQUE INDEX "unique_profile_date_active" ON "diaries" USING btree ("profile_id","date") WHERE "diaries"."is_deleted" = false;