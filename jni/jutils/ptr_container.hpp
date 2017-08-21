// Copyright (c) 2016 Steinwurf ApS
// All Rights Reserved
//
// THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STEINWURF
// The copyright notice above does not evidence any
// actual or intended publication of such source code.

#pragma once

#include <memory>
namespace jutils
{
template<class Object>
class ptr_container
{
public:

    ptr_container(std::shared_ptr<Object> object) :
        m_object(object)
    { }

    std::shared_ptr<Object> operator->()
    {
        return m_object;
    }

private:

    std::shared_ptr<Object> m_object;
};
}
