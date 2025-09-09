import { Link } from "react-router";
import { Separator } from "~/common/components/ui/separator";
import {
  NavigationMenu,
  NavigationMenuContent,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
  NavigationMenuTrigger,
  navigationMenuTriggerStyle,
} from "./ui/navigation-menu";
import { cn } from "~/lib/utils";
import { Button } from "./ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "./ui/dropdown-menu";
import { Avatar, AvatarFallback, AvatarImage } from "./ui/avatar";
import {
  BarChart3Icon,
  BellIcon,
  LogOutIcon,
  MenuIcon,
  SettingsIcon,
  UserIcon,
  XIcon,
} from "lucide-react";
import { useState } from "react";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "./ui/sheet";
import { useAuthContext } from "~/features/auth";

const menus = [
  {
    name: "일기",
    to: "/diary",
    items: [
      {
        name: "감정 일기 쓰기",
        description: "오늘의 감정과 경험을 기록해보세요",
        to: "/diary/new",
      },
      {
        name: "일기 목록",
        description: "작성한 일기들을 확인하고 관리하세요",
        to: "/diary",
      },
      {
        name: "일기 검색",
        description: "날짜, 감정, 태그별로 일기를 검색하세요",
        to: "/diary/search",
      },
    ],
  },
  {
    name: "분석",
    to: "/analytics",
    items: [
      {
        name: "감정 패턴",
        description: "감정 변화 패턴과 트렌드 분석",
        to: "/analytics/emotions",
      },
      {
        name: "통계",
        description: "일기 작성 빈도 및 통계",
        to: "/analytics/stats",
      },
    ],
  },
];

export default function Navigation({
  hasNotifications,
}: {
  hasNotifications: boolean;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const { user, isAuthenticated, signOut } = useAuthContext();

  const MobileMenu = () => (
    <Sheet open={isOpen} onOpenChange={setIsOpen}>
      <SheetTrigger asChild>
        <Button variant='ghost' size='icon' className='md:hidden'>
          <MenuIcon className='size-5' />
        </Button>
      </SheetTrigger>
      <SheetContent side='right' className='w-80'>
        <div className='flex flex-col gap-4 mt-8 px-3'>
          {menus.map(menu => (
            <div key={menu.name} className='space-y-2'>
              <Link
                to={menu.to}
                className='text-lg font-medium block py-2'
                onClick={() => setIsOpen(false)}
              >
                {menu.name}
              </Link>
              {menu.items && (
                <div className='ml-4 space-y-2'>
                  {menu.items.map(item => (
                    <Link
                      key={item.name}
                      to={item.to}
                      className='block text-sm text-muted-foreground py-1 hover:text-foreground'
                      onClick={() => setIsOpen(false)}
                    >
                      {item.name}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          ))}

          <Separator className='my-4' />

          {isAuthenticated ? (
            <div className='space-y-3'>
              <Link
                to='/notifications'
                className='flex items-center gap-3 py-2'
                onClick={() => setIsOpen(false)}
              >
                <BellIcon className='size-4' />
                <span>알림</span>
                {hasNotifications && (
                  <div className='size-2 bg-red-500 rounded-full' />
                )}
              </Link>
              <Link
                to='/dashboard'
                className='flex items-center gap-3 py-2'
                onClick={() => setIsOpen(false)}
              >
                <BarChart3Icon className='size-4' />
                <span>대시보드</span>
              </Link>
              <Link
                to='/profile'
                className='flex items-center gap-3 py-2'
                onClick={() => setIsOpen(false)}
              >
                <UserIcon className='size-4' />
                <span>프로필</span>
              </Link>
              <Link
                to='/settings'
                className='flex items-center gap-3 py-2'
                onClick={() => setIsOpen(false)}
              >
                <SettingsIcon className='size-4' />
                <span>설정</span>
              </Link>
              <Separator className='my-2' />
              <button
                onClick={async () => {
                  await signOut();
                  setIsOpen(false);
                }}
                className='flex items-center gap-3 py-2 text-red-600 w-full text-left'
              >
                <LogOutIcon className='size-4' />
                <span>로그아웃</span>
              </button>
            </div>
          ) : (
            <div className='space-y-3'>
              <Button asChild variant='secondary' className='w-full'>
                <Link to='/login' onClick={() => setIsOpen(false)}>
                  로그인
                </Link>
              </Button>
            </div>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );

  return (
    <nav className='flex px-4 md:px-20 h-16 items-center justify-between backdrop-blur fixed top-0 left-0 right-0 z-50 bg-background/50'>
      <div className='flex items-center'>
        <Link to='/' className='font-bold tracking-tighter text-lg'>
          MINDLOG
        </Link>
        <Separator
          orientation='vertical'
          className='!h-6 mx-4 hidden md:block'
        />
        <NavigationMenu className='hidden md:block'>
          <NavigationMenuList>
            {menus.map(menu => (
              <NavigationMenuItem key={menu.name}>
                {menu.items ? (
                  <>
                    <Link to={menu.to}>
                      <NavigationMenuTrigger>{menu.name}</NavigationMenuTrigger>
                    </Link>
                    <NavigationMenuContent>
                      <ul className='grid w-[600px] font-light gap-3 p-4 grid-cols-2'>
                        {menu.items?.map(item => (
                          <NavigationMenuItem
                            key={item.name}
                            className={cn([
                              "select-none rounded-md transition-colors focus:bg-accent  hover:bg-accent",
                              item.to === "/diary/new" &&
                                "col-span-2 bg-primary/10 hover:bg-primary/20 focus:bg-primary/20",
                            ])}
                          >
                            <NavigationMenuLink>
                              <Link
                                className='p-3 space-y-1 block leading-none no-underline outline-none'
                                to={item.to}
                              >
                                <span className='text-sm font-medium leading-none'>
                                  {item.name}
                                </span>
                                <p className='text-sm leading-snug text-muted-foreground'>
                                  {item.description}
                                </p>
                              </Link>
                            </NavigationMenuLink>
                          </NavigationMenuItem>
                        ))}
                      </ul>
                    </NavigationMenuContent>
                  </>
                ) : (
                  <Link className={navigationMenuTriggerStyle()} to={menu.to}>
                    {menu.name}
                  </Link>
                )}
              </NavigationMenuItem>
            ))}
          </NavigationMenuList>
        </NavigationMenu>
      </div>

      {/* Desktop Menu */}
      <div className='hidden md:flex items-center gap-4'>
        {isAuthenticated ? (
          <>
            <Button size='icon' variant='ghost' asChild className='relative'>
              <Link to='/notifications'>
                <BellIcon className='size-4' />
                {hasNotifications && (
                  <div className='absolute top-1.5 right-1.5 size-2 bg-red-500 rounded-full' />
                )}
              </Link>
            </Button>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Avatar className="cursor-pointer">
                  <AvatarImage src={user?.user_metadata?.avatar_url || ''} />
                  <AvatarFallback>
                    {user?.user_metadata?.full_name?.[0] || 
                     user?.user_metadata?.name?.[0] || 
                     user?.email?.[0]?.toUpperCase() || 'U'}
                  </AvatarFallback>
                </Avatar>
              </DropdownMenuTrigger>
              <DropdownMenuContent className='w-56'>
                <DropdownMenuLabel className='flex flex-col'>
                  <span className='font-medium'>
                    {user?.user_metadata?.full_name || 
                     user?.user_metadata?.name || 
                     user?.email?.split('@')[0] || '사용자'}
                  </span>
                  <span className='text-xs text-muted-foreground'>
                    {user?.email || ''}
                  </span>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuGroup>
                  <DropdownMenuItem asChild className='cursor-pointer'>
                    <Link to='/dashboard'>
                      <BarChart3Icon className='size-4 mr-2' />
                      대시보드
                    </Link>
                  </DropdownMenuItem>
                  <DropdownMenuItem asChild className='cursor-pointer'>
                    <Link to='/profile'>
                      <UserIcon className='size-4 mr-2' />
                      프로필
                    </Link>
                  </DropdownMenuItem>
                  <DropdownMenuItem asChild className='cursor-pointer'>
                    <Link to='/settings'>
                      <SettingsIcon className='size-4 mr-2' />
                      설정
                    </Link>
                  </DropdownMenuItem>
                </DropdownMenuGroup>
                <DropdownMenuSeparator />
                <DropdownMenuItem 
                  className='cursor-pointer'
                  onClick={async () => await signOut()}
                >
                  <LogOutIcon className='size-4 mr-2' />
                  로그아웃
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </>
        ) : (
          <>
            <Button asChild variant='secondary'>
              <Link to='/login'>로그인</Link>
            </Button>
          </>
        )}
      </div>

      {/* Mobile Menu */}
      <MobileMenu />
    </nav>
  );
}
